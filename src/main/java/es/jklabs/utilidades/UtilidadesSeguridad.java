package es.jklabs.utilidades;

import es.jklabs.security.OsProviderConfig;
import es.jklabs.security.SecureMetadata;

public class UtilidadesSeguridad {
    public static OsProviderConfig getOsProviderConfig(SecureMetadata metadata) {
        OsProviderConfig config = metadata.getOsProvider();
        if (config == null) {
            config = new OsProviderConfig();
            config.setServiceName("BeyondSQLExecutor");
            config.setAccountName("master-key");
            metadata.setOsProvider(config);
        }
        if (config.getServiceName() == null) {
            config.setServiceName("BeyondSQLExecutor");
        }
        if (config.getAccountName() == null) {
            config.setAccountName("master-key");
        }
        return config;
    }
}
