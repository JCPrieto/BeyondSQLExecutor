package es.jklabs.security;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProviderSelectionTest {

    @Test
    void selectsHighestPriorityEnabledProvider() {
        SecureMetadata metadata = new SecureMetadata();
        MasterKeyProvider high = new FakeProvider("high", 100, true);
        MasterKeyProvider low = new FakeProvider("low", 10, true);
        ProviderSelector.getConfig(metadata, high).setEnabled(false);
        List<MasterKeyProvider> ordered = ProviderSelector.orderProviders(List.of(low, high), metadata);
        List<String> enabled = ordered.stream()
                .filter(p -> ProviderSelector.isEnabled(metadata, p) && p.isAvailable())
                .map(MasterKeyProvider::getId)
                .collect(Collectors.toList());
        assertEquals("low", enabled.get(0));
    }

    private static class FakeProvider implements MasterKeyProvider {
        private final String id;
        private final int priority;
        private final boolean available;

        private FakeProvider(String id, int priority, boolean available) {
            this.id = id;
            this.priority = priority;
            this.available = available;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDisplayName() {
            return id;
        }

        @Override
        public int getDefaultPriority() {
            return priority;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public byte[] getOrCreateMasterKey(SecureMetadata metadata, Component parent, boolean allowCreate) {
            return null;
        }

        @Override
        public void reset(SecureMetadata metadata, Component parent) {
        }
    }
}
