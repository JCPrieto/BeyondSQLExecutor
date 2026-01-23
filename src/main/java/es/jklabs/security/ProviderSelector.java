package es.jklabs.security;

import java.util.Comparator;
import java.util.List;

public class ProviderSelector {
    private ProviderSelector() {

    }

    public static List<MasterKeyProvider> orderProviders(List<MasterKeyProvider> providers, SecureMetadata metadata) {
        return providers.stream()
                .sorted(Comparator.comparingInt((MasterKeyProvider p) -> -getPriority(metadata, p)))
                .toList();
    }

    public static int getPriority(SecureMetadata metadata, MasterKeyProvider provider) {
        ProviderConfig config = getConfig(metadata, provider);
        return config.getPriority();
    }

    public static boolean isEnabled(SecureMetadata metadata, MasterKeyProvider provider) {
        ProviderConfig config = getConfig(metadata, provider);
        return config.isEnabled();
    }

    public static ProviderConfig getConfig(SecureMetadata metadata, MasterKeyProvider provider) {
        ProviderConfig config = metadata.getProviders().get(provider.getId());
        if (config == null) {
            config = new ProviderConfig(true, provider.getDefaultPriority());
            metadata.getProviders().put(provider.getId(), config);
        }
        return config;
    }
}
