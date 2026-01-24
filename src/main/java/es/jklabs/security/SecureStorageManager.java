package es.jklabs.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.utilidades.Logger;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;

public class SecureStorageManager {
    private static final int SCHEMA_VERSION = 1;
    private static final int VAULT_VERSION = 2;

    private final Path secureDir;
    private final Path vaultPath;
    private final Path metaPath;
    private final Gson gson;
    private final List<MasterKeyProvider> providers;

    private SecureMetadata metadata;
    private SecureVaultFile vault;
    private byte[] cachedMasterKey;
    private String cachedProviderId;

    public SecureStorageManager(Path secureDir) {
        this(secureDir, List.of(
                new LinuxSecretServiceProvider(),
                new WindowsCredentialManagerProvider(),
                new MacKeychainProvider(),
                new UiPromptProvider()
        ));
    }

    public SecureStorageManager(Path secureDir, List<MasterKeyProvider> providers) {
        this.secureDir = secureDir;
        this.vaultPath = secureDir.resolve("credentials-config.json");
        this.metaPath = secureDir.resolve("secure-meta.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.providers = providers;
    }

    public void load() {
        try {
            if (!Files.exists(secureDir)) {
                Files.createDirectories(secureDir);
            }
            if (Files.exists(metaPath)) {
                metadata = gson.fromJson(Files.readString(metaPath, StandardCharsets.UTF_8), SecureMetadata.class);
            }
            if (metadata == null) {
                metadata = new SecureMetadata();
            }
            metadata.setSchemaVersion(SCHEMA_VERSION);
            ensureProviderDefaults(metadata);
            if (Files.exists(vaultPath)) {
                vault = gson.fromJson(Files.readString(vaultPath, StandardCharsets.UTF_8), SecureVaultFile.class);
            }
            if (vault == null) {
                vault = new SecureVaultFile();
            }
            vault.setVaultVersion(VAULT_VERSION);
            save();
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public void save() {
        try {
            Files.writeString(metaPath, gson.toJson(metadata), StandardCharsets.UTF_8);
            Files.writeString(vaultPath, gson.toJson(vault), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public SecureMetadata getMetadata() {
        return metadata;
    }

    public SecureVaultFile getVault() {
        return vault;
    }

    public void clearCachedMasterKey() {
        CryptoUtils.wipe(cachedMasterKey);
        cachedMasterKey = null;
        cachedProviderId = null;
    }

    public String getPassword(String credentialRef, Component parent) throws SecureStorageException {
        CredentialData data = loadCredential(credentialRef, parent);
        return data != null ? data.getPassword() : null;
    }

    public void setPassword(String credentialRef, String password, Component parent) throws SecureStorageException {
        if (credentialRef == null || password == null) {
            return;
        }
        CredentialData data = new CredentialData(password);
        storeCredential(credentialRef, data, parent);
    }

    public void setPassword(String credentialRef, char[] password, Component parent) throws SecureStorageException {
        if (credentialRef == null || password == null) {
            return;
        }
        String plain = null;
        try {
            plain = new String(password);
            CredentialData data = new CredentialData(plain);
            storeCredential(credentialRef, data, parent);
        } finally {
            CryptoUtils.wipe(password);
        }
    }

    public void removeCredential(String credentialRef) {
        if (credentialRef != null) {
            vault.getEntries().remove(credentialRef);
            save();
        }
    }

    public void changeMasterProvider(String providerId, Component parent) throws SecureStorageException {
        Map<String, CredentialData> decrypted = decryptAll(parent);
        clearCachedMasterKey();
        byte[] newKey = unlockWithProvider(providerId, parent, true);
        if (newKey == null) {
            throw new SecureStorageException("No se pudo desbloquear el proveedor seleccionado.");
        }
        try {
            reencryptAll(decrypted, newKey, providerId);
        } finally {
            CryptoUtils.wipe(newKey);
        }
        save();
    }

    public void recoverUiProvider(Component parent) throws SecureStorageException {
        int result = javax.swing.JOptionPane.showConfirmDialog(parent,
                "Se va a reiniciar el almacenamiento seguro y se perderan las credenciales.",
                "Recuperar almacenamiento seguro", javax.swing.JOptionPane.OK_CANCEL_OPTION);
        if (result != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }
        vault.getEntries().clear();
        metadata.setUiKdfParams(null);
        clearCachedMasterKey();
        save();
    }

    private CredentialData loadCredential(String credentialRef, Component parent) throws SecureStorageException {
        SecureVaultEntry entry = vault.getEntries().get(credentialRef);
        if (entry == null) {
            return null;
        }
        byte[] key = resolveMasterKey(parent, false);
        if (key == null) {
            throw new SecureStorageException("No hay proveedor de clave maestra disponible.");
        }
        byte[] aad = credentialRef.getBytes(StandardCharsets.UTF_8);
        byte[] nonce = Base64.getDecoder().decode(entry.getNonceB64());
        byte[] ciphertext = Base64.getDecoder().decode(entry.getCiphertextB64());
        try {
            return decryptPayload(key, aad, nonce, ciphertext);
        } catch (GeneralSecurityException e) {
            clearCachedMasterKey();
            byte[] fallbackKey = resolveMasterKey(parent, false);
            if (fallbackKey != null && !Arrays.equals(key, fallbackKey)) {
                try {
                    return decryptPayload(fallbackKey, aad, nonce, ciphertext);
                } catch (GeneralSecurityException ex) {
                    throw new SecureStorageException("Error al descifrar la credencial.", ex);
                }
            }
            throw new SecureStorageException("Error al descifrar la credencial.", e);
        }
    }

    private CredentialData decryptPayload(byte[] key, byte[] aad, byte[] nonce, byte[] ciphertext)
            throws GeneralSecurityException {
        byte[] plaintext = CryptoUtils.decryptAesGcm(key, aad, nonce, ciphertext);
        String json = new String(plaintext, StandardCharsets.UTF_8);
        CryptoUtils.wipe(plaintext);
        return gson.fromJson(json, CredentialData.class);
    }

    private void storeCredential(String credentialRef, CredentialData data, Component parent) throws SecureStorageException {
        byte[] key = resolveMasterKey(parent, true);
        if (key == null) {
            throw new SecureStorageException("No hay proveedor de clave maestra disponible.");
        }
        byte[] aad = credentialRef.getBytes(StandardCharsets.UTF_8);
        String json = gson.toJson(data);
        try {
            CryptoUtils.AesGcmPayload payload = CryptoUtils.encryptAesGcm(key, aad, json.getBytes(StandardCharsets.UTF_8));
            String nonceB64 = Base64.getEncoder().encodeToString(payload.nonce());
            String cipherB64 = Base64.getEncoder().encodeToString(payload.ciphertext());
            vault.getEntries().put(credentialRef, new SecureVaultEntry(nonceB64, cipherB64));
            save();
        } catch (GeneralSecurityException e) {
            throw new SecureStorageException("Error al cifrar la credencial.", e);
        }
    }

    private Map<String, CredentialData> decryptAll(Component parent) throws SecureStorageException {
        Map<String, CredentialData> result = new HashMap<>();
        for (String key : vault.getEntries().keySet()) {
            CredentialData data = loadCredential(key, parent);
            if (data != null) {
                result.put(key, data);
            }
        }
        return result;
    }

    private void reencryptAll(Map<String, CredentialData> decrypted, byte[] masterKey, String providerId)
            throws SecureStorageException {
        vault.getEntries().clear();
        cachedMasterKey = Arrays.copyOf(masterKey, masterKey.length);
        cachedProviderId = providerId;
        for (Map.Entry<String, CredentialData> entry : decrypted.entrySet()) {
            storeCredential(entry.getKey(), entry.getValue(), null);
        }
        metadata.setProviderHint(providerId);
    }

    private byte[] resolveMasterKey(Component parent, boolean allowCreate) throws SecureStorageException {
        if (cachedMasterKey != null && cachedProviderId != null) {
            return cachedMasterKey;
        }
        List<MasterKeyProvider> ordered = getOrderedProviders();
        if (metadata.getProviderHint() != null) {
            for (MasterKeyProvider provider : ordered) {
                if (provider.getId().equals(metadata.getProviderHint())) {
                    byte[] key = unlockWithProvider(provider.getId(), parent, allowCreate);
                    if (key != null) {
                        return key;
                    }
                }
            }
        }
        for (MasterKeyProvider provider : ordered) {
            byte[] key = unlockWithProvider(provider.getId(), parent, allowCreate);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    private byte[] unlockWithProvider(String providerId, Component parent, boolean allowCreate)
            throws SecureStorageException {
        MasterKeyProvider provider = getProviderById(providerId);
        if (provider == null) {
            return null;
        }
        ProviderConfig config = ProviderSelector.getConfig(metadata, provider);
        if (!config.isEnabled() || !provider.isAvailable()) {
            return null;
        }
        byte[] key = provider.getOrCreateMasterKey(metadata, parent, allowCreate);
        if (key == null || key.length == 0) {
            return null;
        }
        cachedMasterKey = Arrays.copyOf(key, key.length);
        cachedProviderId = providerId;
        metadata.setProviderHint(providerId);
        return cachedMasterKey;
    }

    private List<MasterKeyProvider> getOrderedProviders() {
        return ProviderSelector.orderProviders(providers, metadata);
    }

    public List<MasterKeyProvider> getProviders() {
        return providers;
    }

    public ProviderConfig ensureProviderConfig(SecureMetadata metadata, MasterKeyProvider provider) {
        return ProviderSelector.getConfig(metadata, provider);
    }

    public int getPriority(MasterKeyProvider provider) {
        return ProviderSelector.getPriority(metadata, provider);
    }

    public void updateProviderConfig(String providerId, boolean enabled, int priority) {
        MasterKeyProvider provider = getProviderById(providerId);
        if (provider == null) {
            return;
        }
        ProviderConfig config = ensureProviderConfig(metadata, provider);
        config.setEnabled(enabled);
        config.setPriority(priority);
        save();
    }

    private MasterKeyProvider getProviderById(String providerId) {
        for (MasterKeyProvider provider : providers) {
            if (provider.getId().equals(providerId)) {
                return provider;
            }
        }
        return null;
    }

    private void ensureProviderDefaults(SecureMetadata metadata) {
        for (MasterKeyProvider provider : providers) {
            ProviderSelector.getConfig(metadata, provider);
        }
    }
}
