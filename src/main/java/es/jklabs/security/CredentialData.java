package es.jklabs.security;

public class CredentialData {
    private String password;

    public CredentialData() {
    }

    public CredentialData(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
