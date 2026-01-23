package es.jklabs.security;

import java.awt.*;

public interface MasterKeyProvider {
    String getId();

    String getDisplayName();

    int getDefaultPriority();

    boolean isAvailable();

    byte[] getOrCreateMasterKey(SecureMetadata metadata, Component parent, boolean allowCreate)
            throws SecureStorageException;

    void reset(SecureMetadata metadata, Component parent) throws SecureStorageException;
}
