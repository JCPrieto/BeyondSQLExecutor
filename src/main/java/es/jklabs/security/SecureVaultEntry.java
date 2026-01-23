package es.jklabs.security;

public class SecureVaultEntry {
    private String nonceB64;
    private String ciphertextB64;

    public SecureVaultEntry() {
    }

    public SecureVaultEntry(String nonceB64, String ciphertextB64) {
        this.nonceB64 = nonceB64;
        this.ciphertextB64 = ciphertextB64;
    }

    public String getNonceB64() {
        return nonceB64;
    }

    public void setNonceB64(String nonceB64) {
        this.nonceB64 = nonceB64;
    }

    public String getCiphertextB64() {
        return ciphertextB64;
    }

    public void setCiphertextB64(String ciphertextB64) {
        this.ciphertextB64 = ciphertextB64;
    }
}
