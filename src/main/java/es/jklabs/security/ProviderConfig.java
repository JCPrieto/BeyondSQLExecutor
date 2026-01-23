package es.jklabs.security;

public class ProviderConfig {
    private boolean enabled;
    private int priority;

    public ProviderConfig() {
    }

    public ProviderConfig(boolean enabled, int priority) {
        this.enabled = enabled;
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
