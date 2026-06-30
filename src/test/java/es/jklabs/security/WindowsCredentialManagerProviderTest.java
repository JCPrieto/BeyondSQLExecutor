package es.jklabs.security;

import com.sun.jna.Memory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WindowsCredentialManagerProviderTest {

    @Test
    void getOrCreateMasterKeyReturnsDecodedExistingKey() throws Exception {
        byte[] existingKey = "existing-secret".getBytes(StandardCharsets.UTF_8);
        TrackingCredentialStore store = TrackingCredentialStore.withRead(
                Base64.getEncoder().encode(existingKey)
        );
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(store);

        byte[] key = provider.getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertArrayEquals(existingKey, key);
        assertEquals(List.of("BeyondSQLExecutor Master Key"), store.readTargets);
        assertTrue(store.writes.isEmpty());
    }

    @Test
    void getOrCreateMasterKeyReturnsRawExistingKeyWhenStoredBlobIsNotBase64() throws Exception {
        byte[] rawKey = "!not-base64!".getBytes(StandardCharsets.UTF_8);
        TrackingCredentialStore store = TrackingCredentialStore.withRead(rawKey);
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(store);

        byte[] key = provider.getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertArrayEquals(rawKey, key);
        assertTrue(store.writes.isEmpty());
    }

    @Test
    void getOrCreateMasterKeyReturnsNullWhenCredentialIsMissingAndCreationIsNotAllowed() throws Exception {
        TrackingCredentialStore store = TrackingCredentialStore.withRead(null);
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(store);

        byte[] key = provider.getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertNull(key);
        assertEquals(1, store.readTargets.size());
        assertTrue(store.writes.isEmpty());
    }

    @Test
    void getOrCreateMasterKeyCreatesAndStoresKeyWithDefaultConfig() throws Exception {
        TrackingCredentialStore store = TrackingCredentialStore.withRead(null);
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(store);
        SecureMetadata metadata = new SecureMetadata();

        byte[] key = provider.getOrCreateMasterKey(metadata, null, true);

        assertEquals(32, key.length);
        assertEquals("BeyondSQLExecutor Master Key", metadata.getOsProvider().getTargetName());
        assertEquals("master-key", metadata.getOsProvider().getAccountName());
        assertEquals(1, store.writes.size());
        CredentialWrite write = store.writes.getFirst();
        assertEquals("BeyondSQLExecutor Master Key", write.target());
        assertEquals("master-key", write.userName());
        assertArrayEquals(key, Base64.getDecoder().decode(write.secretBytes()));
    }

    @Test
    void getOrCreateMasterKeyCompletesPartialExistingConfigBeforeWriting() throws Exception {
        TrackingCredentialStore store = TrackingCredentialStore.withRead(null);
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(store);
        SecureMetadata metadata = new SecureMetadata();
        OsProviderConfig config = new OsProviderConfig();
        config.setTargetName("custom-target");
        metadata.setOsProvider(config);

        provider.getOrCreateMasterKey(metadata, null, true);

        assertEquals("custom-target", config.getTargetName());
        assertEquals("master-key", config.getAccountName());
        CredentialWrite write = store.writes.getFirst();
        assertEquals("custom-target", write.target());
        assertEquals("master-key", write.userName());
    }

    @Test
    void getOrCreateMasterKeyCompletesMissingTargetWithoutReplacingExistingAccount() throws Exception {
        TrackingCredentialStore store = TrackingCredentialStore.withRead(null);
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(store);
        SecureMetadata metadata = new SecureMetadata();
        OsProviderConfig config = new OsProviderConfig();
        config.setAccountName("custom-account");
        metadata.setOsProvider(config);

        provider.getOrCreateMasterKey(metadata, null, true);

        assertEquals("BeyondSQLExecutor Master Key", config.getTargetName());
        assertEquals("custom-account", config.getAccountName());
        CredentialWrite write = store.writes.getFirst();
        assertEquals("BeyondSQLExecutor Master Key", write.target());
        assertEquals("custom-account", write.userName());
    }

    @Test
    void exposesProviderMetadata() {
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(TrackingCredentialStore.withRead(null));

        assertEquals(WindowsCredentialManagerProvider.ID, provider.getId());
        assertEquals("Windows Credential Manager", provider.getDisplayName());
        assertEquals(100, provider.getDefaultPriority());
        assertDoesNotThrow(() -> provider.reset(new SecureMetadata(), null));
    }

    @Test
    void getOrCreateMasterKeyWrapsCredentialStoreErrors() {
        WindowsCredentialManagerProvider provider = new WindowsCredentialManagerProvider(new FailingCredentialStore());

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider.getOrCreateMasterKey(new SecureMetadata(), null, false));

        assertEquals("No se pudo acceder al Administrador de credenciales.", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void credentialReturnsNullWhenBlobPointerIsNull() {
        WindowsCredentialManagerProvider.CREDENTIAL credential = new WindowsCredentialManagerProvider.CREDENTIAL();
        credential.CredentialBlobSize = 10;

        assertNull(credential.readCredentialBlob());
    }

    @Test
    void credentialReturnsNullWhenBlobSizeIsNotPositive() {
        WindowsCredentialManagerProvider.CREDENTIAL credential = new WindowsCredentialManagerProvider.CREDENTIAL();
        credential.CredentialBlob = new Memory(1);
        credential.CredentialBlobSize = 0;

        assertNull(credential.readCredentialBlob());
    }

    @Test
    void credentialReadsBlobBytesWhenPointerAndSizeAreValid() {
        byte[] secret = "secret".getBytes(StandardCharsets.UTF_8);
        WindowsCredentialManagerProvider.CREDENTIAL credential = new WindowsCredentialManagerProvider.CREDENTIAL();
        credential.CredentialBlob = new Memory(secret.length);
        credential.CredentialBlob.write(0, secret, 0, secret.length);
        credential.CredentialBlobSize = secret.length;

        assertArrayEquals(secret, credential.readCredentialBlob());
    }

    private static final class TrackingCredentialStore implements WindowsCredentialManagerProvider.CredentialStore {
        private final byte[] readResult;
        private final List<String> readTargets = new ArrayList<>();
        private final List<CredentialWrite> writes = new ArrayList<>();

        private TrackingCredentialStore(byte[] readResult) {
            this.readResult = readResult;
        }

        private static TrackingCredentialStore withRead(byte[] readResult) {
            return new TrackingCredentialStore(readResult);
        }

        @Override
        public byte[] read(String target) {
            readTargets.add(target);
            return readResult;
        }

        @Override
        public void write(String target, String userName, byte[] secretBytes) {
            writes.add(new CredentialWrite(target, userName, secretBytes));
        }
    }

    private static final class FailingCredentialStore implements WindowsCredentialManagerProvider.CredentialStore {
        @Override
        public byte[] read(String target) {
            throw new IllegalStateException("boom");
        }

        @Override
        public void write(String target, String userName, byte[] secretBytes) {
            fail("write should not be called");
        }
    }

    private record CredentialWrite(String target, String userName, byte[] secretBytes) {
    }
}
