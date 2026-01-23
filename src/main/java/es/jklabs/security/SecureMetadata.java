package es.jklabs.security;

import java.util.HashMap;
import java.util.Map;

public class SecureMetadata {
    private int schemaVersion;
    private Map<String, ProviderConfig> providers;
    private String providerHint;
    private UiKdfParams uiKdfParams;
    private OsProviderConfig osProvider;
    private String migrationMarker;

    public SecureMetadata() {
        providers = new HashMap<>();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers;
    }

    public String getProviderHint() {
        return providerHint;
    }

    public void setProviderHint(String providerHint) {
        this.providerHint = providerHint;
    }

    public UiKdfParams getUiKdfParams() {
        return uiKdfParams;
    }

    public void setUiKdfParams(UiKdfParams uiKdfParams) {
        this.uiKdfParams = uiKdfParams;
    }

    public OsProviderConfig getOsProvider() {
        return osProvider;
    }

    public void setOsProvider(OsProviderConfig osProvider) {
        this.osProvider = osProvider;
    }

    public String getMigrationMarker() {
        return migrationMarker;
    }

    public void setMigrationMarker(String migrationMarker) {
        this.migrationMarker = migrationMarker;
    }
}
