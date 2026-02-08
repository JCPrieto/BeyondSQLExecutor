package es.jklabs.storage;

import com.google.gson.*;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.security.SecureMetadata;
import es.jklabs.security.SecureStorageManager;
import es.jklabs.security.SecureVaultEntry;
import es.jklabs.security.SecureVaultFile;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.UtilidadesEncryptacion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileSystemProjectStore implements ProjectStore {
    private static final String CONNECTIONS_JSON = "connections.json";
    private static final String SECURE_DIR = ".secure";
    private static final String VAULT_FILE = "credentials-config.json";
    private static final String META_FILE = "secure-meta.json";

    private final Path baseDir;
    private final Path connectionsPath;
    private final Path secureDir;
    private final Function<Path, SecureStorageManager> secureStorageManagerFactory;
    private final Gson gsonRead;
    private final Gson gsonWrite;
    private StoreError lastError;

    public FileSystemProjectStore(Path baseDir) {
        this(baseDir, SecureStorageManager::new);
    }

    FileSystemProjectStore(Path baseDir, Function<Path, SecureStorageManager> secureStorageManagerFactory) {
        this.baseDir = baseDir;
        this.connectionsPath = baseDir.resolve(CONNECTIONS_JSON);
        this.secureDir = baseDir.resolve(SECURE_DIR);
        this.secureStorageManagerFactory = secureStorageManagerFactory;
        this.gsonRead = new GsonBuilder().create();
        this.gsonWrite = new GsonBuilder()
                .setPrettyPrinting()
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return Objects.equals(f.getName(), "pass");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();
    }

    @Override
    public Configuracion load() {
        lastError = null;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            Logger.error(e);
        }
        if (!Files.exists(connectionsPath)) {
            return new Configuracion();
        }
        try {
            return gsonRead.fromJson(Files.readString(connectionsPath, StandardCharsets.UTF_8), Configuracion.class);
        } catch (JsonSyntaxException e) {
            handleCorruptJson(connectionsPath, true, e);
            return new Configuracion();
        } catch (Exception e) {
            Logger.error(e);
            return new Configuracion();
        }
    }

    @Override
    public void save(Configuracion configuracion) {
        try {
            Files.writeString(connectionsPath, gsonWrite.toJson(configuracion), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    @Override
    public Configuracion importProject(File file, Configuracion existing) {
        lastError = null;
        if (file == null) {
            return existing;
        }
        String name = file.getName().toLowerCase();
        if (name.endsWith(".zip")) {
            return importZip(file, existing);
        }
        if (name.endsWith(".json")) {
            return importLegacyJson(file, existing);
        }
        return existing;
    }

    @Override
    public void exportProject(File file) {
        if (file == null) {
            return;
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            if (Files.exists(connectionsPath)) {
                addZipEntry(zos, buildPortableConnectionsJson());
            }
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private byte[] buildPortableConnectionsJson() {
        try {
            Configuracion config = gsonRead.fromJson(Files.readString(connectionsPath, StandardCharsets.UTF_8),
                    Configuracion.class);
            if (config == null || config.getServers() == null || config.getServers().isEmpty()) {
                return Files.readAllBytes(connectionsPath);
            }
            SecureStorageManager manager = null;
            for (Servidor servidor : config.getServers()) {
                if (servidor == null || servidor.getTipoLogin() != TipoLogin.USUARIO_CONTRASENA) {
                    continue;
                }
                String credentialRef = servidor.getCredentialRef();
                if (credentialRef == null || credentialRef.isBlank()) {
                    continue;
                }
                if (manager == null) {
                    if (!Files.exists(secureDir.resolve(VAULT_FILE))) {
                        continue;
                    }
                    manager = secureStorageManagerFactory.apply(secureDir);
                    manager.load();
                }
                try {
                    String plain = manager.getPassword(credentialRef, null);
                    if (plain != null) {
                        servidor.setPass(UtilidadesEncryptacion.encryptLegacy(plain));
                        servidor.setCredentialRef(null);
                    }
                } catch (Exception ex) {
                    Logger.error(ex);
                }
            }
            return gsonRead.toJson(config).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.error(e);
            try {
                return Files.readAllBytes(connectionsPath);
            } catch (IOException ex) {
                Logger.error(ex);
                return new byte[0];
            }
        }
    }

    private Configuracion importZip(File file, Configuracion existing) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("bse-import-");
            unzip(file.toPath(), tempDir);
            Path importedConnections = tempDir.resolve(CONNECTIONS_JSON);
            if (Files.exists(importedConnections)) {
                Configuracion imported;
                try {
                    imported = gsonRead.fromJson(
                            Files.readString(importedConnections, StandardCharsets.UTF_8),
                            Configuracion.class);
                } catch (JsonSyntaxException e) {
                    handleCorruptJson(importedConnections, false, e);
                    imported = null;
                }
                if (imported != null && imported.getServers() != null) {
                    imported.getServers().forEach(server -> {
                        if (existing.getServers().stream().noneMatch(s -> Objects.equals(s, server))) {
                            existing.getServers().add(server);
                        }
                    });
                }
            }
            mergeSecureFiles(tempDir);
            return existing;
        } catch (Exception e) {
            Logger.error(e);
            return existing;
        } finally {
            if (tempDir != null) {
                try {
                    deleteRecursive(tempDir);
                } catch (IOException e) {
                    Logger.error(e);
                }
            }
        }
    }

    private Configuracion importLegacyJson(File file, Configuracion existing) {
        try {
            Configuracion imported;
            try {
                imported = gsonRead.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                        Configuracion.class);
            } catch (JsonSyntaxException e) {
                handleCorruptJson(file.toPath(), false, e);
                imported = null;
            }
            if (imported != null && imported.getServers() != null) {
                imported.getServers().forEach(server -> {
                    if (existing.getServers().stream().noneMatch(s -> Objects.equals(s, server))) {
                        existing.getServers().add(server);
                    }
                });
            }
            return existing;
        } catch (Exception e) {
            Logger.error(e);
            return existing;
        }
    }

    private void mergeSecureFiles(Path importDir) {
        Path importSecure = importDir.resolve(SECURE_DIR);
        if (!Files.exists(importSecure)) {
            return;
        }
        try {
            Files.createDirectories(secureDir);
            Path importVaultPath = importSecure.resolve(VAULT_FILE);
            Path importMetaPath = importSecure.resolve(META_FILE);
            if (!Files.exists(secureDir.resolve(VAULT_FILE)) && Files.exists(importVaultPath)) {
                Files.copy(importVaultPath, secureDir.resolve(VAULT_FILE), StandardCopyOption.REPLACE_EXISTING);
            } else if (Files.exists(importVaultPath)) {
                SecureVaultFile currentVault;
                try {
                    currentVault = gsonRead.fromJson(
                            Files.readString(secureDir.resolve(VAULT_FILE), StandardCharsets.UTF_8),
                            SecureVaultFile.class);
                } catch (JsonSyntaxException e) {
                    handleCorruptJson(secureDir.resolve(VAULT_FILE), true, e);
                    currentVault = new SecureVaultFile();
                }
                SecureVaultFile importVault;
                try {
                    importVault = gsonRead.fromJson(
                            Files.readString(importVaultPath, StandardCharsets.UTF_8),
                            SecureVaultFile.class);
                } catch (JsonSyntaxException e) {
                    handleCorruptJson(importVaultPath, false, e);
                    importVault = null;
                }
                if (currentVault == null) {
                    currentVault = new SecureVaultFile();
                }
                if (currentVault.getEntries() == null) {
                    currentVault.setEntries(new HashMap<>());
                }
                if (importVault != null && importVault.getEntries() != null) {
                    for (Map.Entry<String, SecureVaultEntry> entry : importVault.getEntries().entrySet()) {
                        currentVault.getEntries().putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
                Files.writeString(secureDir.resolve(VAULT_FILE), gsonWrite.toJson(currentVault), StandardCharsets.UTF_8);
            }
            if (!Files.exists(secureDir.resolve(META_FILE)) && Files.exists(importMetaPath)) {
                Files.copy(importMetaPath, secureDir.resolve(META_FILE), StandardCopyOption.REPLACE_EXISTING);
            } else if (Files.exists(importMetaPath)) {
                SecureMetadata currentMeta;
                try {
                    currentMeta = gsonRead.fromJson(
                            Files.readString(secureDir.resolve(META_FILE), StandardCharsets.UTF_8),
                            SecureMetadata.class);
                } catch (JsonSyntaxException e) {
                    handleCorruptJson(secureDir.resolve(META_FILE), true, e);
                    currentMeta = new SecureMetadata();
                }
                SecureMetadata importMeta;
                try {
                    importMeta = gsonRead.fromJson(
                            Files.readString(importMetaPath, StandardCharsets.UTF_8),
                            SecureMetadata.class);
                } catch (JsonSyntaxException e) {
                    handleCorruptJson(importMetaPath, false, e);
                    importMeta = null;
                }
                if (currentMeta == null) {
                    currentMeta = importMeta;
                } else if (importMeta != null && currentMeta.getUiKdfParams() == null) {
                    currentMeta.setUiKdfParams(importMeta.getUiKdfParams());
                }
                Files.writeString(secureDir.resolve(META_FILE), gsonWrite.toJson(currentMeta), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private void unzip(Path zipPath, Path destination) throws IOException {
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = normalizedDestination.resolve(entry.getName()).normalize();
                if (!outPath.startsWith(normalizedDestination)) {
                    throw new IOException("Invalid ZIP entry path: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Path parent = outPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private void addZipEntry(ZipOutputStream zos, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(FileSystemProjectStore.CONNECTIONS_JSON);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        Logger.error(e);
                    }
                });
    }

    public Path getConnectionsPath() {
        return connectionsPath;
    }

    public Path getSecureDir() {
        return secureDir;
    }

    public StoreError consumeLastError() {
        StoreError error = lastError;
        lastError = null;
        return error;
    }

    private void handleCorruptJson(Path path, boolean backup, Exception e) {
        lastError = new StoreError("configuracion.corrupta", new String[]{path.toString()}, e);
        if (!backup) {
            return;
        }
        try {
            if (!Files.exists(path)) {
                return;
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            Path target = path.resolveSibling(path.getFileName() + ".corrupt-" + timestamp + ".bak");
            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    public record StoreError(String key, String[] params, Exception exception) {
    }
}
