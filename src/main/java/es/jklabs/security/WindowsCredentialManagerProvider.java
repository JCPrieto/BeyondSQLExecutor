package es.jklabs.security;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class WindowsCredentialManagerProvider implements MasterKeyProvider {
    public static final String ID = "os-credential-manager";
    static final int CRED_TYPE_GENERIC = 1;
    static final int CRED_PERSIST_LOCAL_MACHINE = 2;
    private final CredentialStore credentialStore;

    public WindowsCredentialManagerProvider() {
        this(new WindowsCredentialStore());
    }

    WindowsCredentialManagerProvider(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Windows Credential Manager";
    }

    @Override
    public int getDefaultPriority() {
        return 100;
    }

    @Override
    public boolean isAvailable() {
        return OsUtils.isWindows();
    }

    @Override
    public byte[] getOrCreateMasterKey(SecureMetadata metadata, Component parent, boolean allowCreate)
            throws SecureStorageException {
        OsProviderConfig config = ensureConfig(metadata);
        String target = config.getTargetName();
        try {
            byte[] existing = readCredential(target);
            if (existing != null) {
                return existing;
            }
            if (!allowCreate) {
                return null;
            }
            byte[] key = CryptoUtils.randomBytes(32);
            writeCredential(target, config.getAccountName(), key);
            return key;
        } catch (Exception e) {
            throw new SecureStorageException("No se pudo acceder al Administrador de credenciales.", e);
        }
    }

    @Override
    public void reset(SecureMetadata metadata, Component parent) {
        // Best-effort; a new key will be created on next unlock.
    }

    private OsProviderConfig ensureConfig(SecureMetadata metadata) {
        OsProviderConfig config = metadata.getOsProvider();
        if (config == null) {
            config = new OsProviderConfig();
            config.setTargetName("BeyondSQLExecutor Master Key");
            config.setAccountName("master-key");
            metadata.setOsProvider(config);
        }
        if (config.getTargetName() == null) {
            config.setTargetName("BeyondSQLExecutor Master Key");
        }
        if (config.getAccountName() == null) {
            config.setAccountName("master-key");
        }
        return config;
    }

    private byte[] readCredential(String target) {
        byte[] result = credentialStore.read(target);
        if (result == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(new String(result, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return result;
        }
    }

    private void writeCredential(String target, String userName, byte[] secret) {
        String encoded = Base64.getEncoder().encodeToString(secret);
        credentialStore.write(target, userName, encoded.getBytes(StandardCharsets.UTF_8));
    }

    interface CredentialStore {
        byte[] read(String target);

        void write(String target, String userName, byte[] secretBytes);
    }

    public interface Advapi32 extends StdCallLibrary {
        Advapi32 INSTANCE = Native.load("Advapi32", Advapi32.class, W32APIOptions.UNICODE_OPTIONS);

        boolean CredRead(WString targetName, int type, int flags, PointerByReference pCredential);

        void CredWrite(CREDENTIAL credential, int flags);

        void CredFree(Pointer credential);
    }

    public static class CREDENTIAL extends Structure {
        public int Flags;
        public int Type;
        public WString TargetName;
        public WString Comment;
        public WinBase.FILETIME LastWritten;
        public int CredentialBlobSize;
        public Pointer CredentialBlob;
        public int Persist;
        public int AttributeCount;
        public Pointer Attributes;
        public WString TargetAlias;
        public WString UserName;

        public CREDENTIAL() {
            super();
        }

        public CREDENTIAL(Pointer p) {
            super(p);
        }

        @Override
        protected List<String> getFieldOrder() {
            return List.of("Flags", "Type", "TargetName", "Comment", "LastWritten", "CredentialBlobSize",
                    "CredentialBlob", "Persist", "AttributeCount", "Attributes", "TargetAlias", "UserName");
        }

        public byte[] readCredentialBlob() {
            if (CredentialBlob == null || CredentialBlobSize <= 0) {
                return null;
            }
            return CredentialBlob.getByteArray(0, CredentialBlobSize);
        }
    }
}
