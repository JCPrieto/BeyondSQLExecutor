package es.jklabs.storage;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.security.SecureMetadata;
import es.jklabs.security.SecureVaultEntry;
import es.jklabs.security.SecureVaultFile;
import es.jklabs.utilidades.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    private final Gson gsonRead;
    private final Gson gsonWrite;

    public FileSystemProjectStore(Path baseDir) {
        this.baseDir = baseDir;
        this.connectionsPath = baseDir.resolve(CONNECTIONS_JSON);
        this.secureDir = baseDir.resolve(SECURE_DIR);
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
                addZipEntry(zos, CONNECTIONS_JSON, Files.readAllBytes(connectionsPath));
            }
            if (Files.exists(secureDir)) {
                Path vault = secureDir.resolve(VAULT_FILE);
                Path meta = secureDir.resolve(META_FILE);
                if (Files.exists(vault)) {
                    addZipEntry(zos, SECURE_DIR + "/" + VAULT_FILE, Files.readAllBytes(vault));
                }
                if (Files.exists(meta)) {
                    addZipEntry(zos, SECURE_DIR + "/" + META_FILE, Files.readAllBytes(meta));
                }
            }
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private Configuracion importZip(File file, Configuracion existing) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("bse-import-");
            unzip(file.toPath(), tempDir);
            Path importedConnections = tempDir.resolve(CONNECTIONS_JSON);
            if (Files.exists(importedConnections)) {
                Configuracion imported = gsonRead.fromJson(Files.readString(importedConnections, StandardCharsets.UTF_8),
                        Configuracion.class);
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
            Configuracion imported = gsonRead.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8),
                    Configuracion.class);
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
                SecureVaultFile currentVault = gsonRead.fromJson(
                        Files.readString(secureDir.resolve(VAULT_FILE), StandardCharsets.UTF_8),
                        SecureVaultFile.class);
                SecureVaultFile importVault = gsonRead.fromJson(
                        Files.readString(importVaultPath, StandardCharsets.UTF_8),
                        SecureVaultFile.class);
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
                SecureMetadata currentMeta = gsonRead.fromJson(
                        Files.readString(secureDir.resolve(META_FILE), StandardCharsets.UTF_8),
                        SecureMetadata.class);
                SecureMetadata importMeta = gsonRead.fromJson(
                        Files.readString(importMetaPath, StandardCharsets.UTF_8),
                        SecureMetadata.class);
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
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    byte[] data = zis.readAllBytes();
                    Files.write(outPath, data);
                }
            }
        }
    }

    private void addZipEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
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
}
