package es.jklabs.security;

import java.util.HashMap;
import java.util.Map;

public class SecureVaultFile {
    private int vaultVersion;
    private Map<String, SecureVaultEntry> entries;

    public SecureVaultFile() {
        entries = new HashMap<>();
    }

    public int getVaultVersion() {
        return vaultVersion;
    }

    public void setVaultVersion(int vaultVersion) {
        this.vaultVersion = vaultVersion;
    }

    public Map<String, SecureVaultEntry> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, SecureVaultEntry> entries) {
        this.entries = entries;
    }
}
