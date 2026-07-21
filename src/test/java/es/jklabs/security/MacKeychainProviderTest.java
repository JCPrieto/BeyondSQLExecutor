package es.jklabs.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MacKeychainProviderTest {

    private static MacKeychainProvider provider(MacKeychainProvider.CommandExecutor executor) {
        return new MacKeychainProvider(executor, () -> true);
    }

    private static CommandRunner.CommandResult result(int exitCode, String stdout, String stderr) {
        return new CommandRunner.CommandResult(exitCode, stdout, stderr);
    }

    @Test
    void exposesProviderMetadata() {
        MacKeychainProvider provider = new MacKeychainProvider();

        assertEquals(MacKeychainProvider.ID, provider.getId());
        assertEquals("macOS Keychain", provider.getDisplayName());
        assertEquals(100, provider.getDefaultPriority());
        assertDoesNotThrow(() -> provider.reset(new SecureMetadata(), null));
    }

    @Test
    void isAvailableReturnsFalseOutsideMacOsWithoutExecutingCommand() {
        TrackingCommandExecutor executor = new TrackingCommandExecutor();
        MacKeychainProvider provider = new MacKeychainProvider(executor, () -> false);

        assertFalse(provider.isAvailable());
        assertTrue(executor.calls.isEmpty());
    }

    @Test
    void isAvailableReflectsSecurityCommandExitCode() {
        TrackingCommandExecutor availableExecutor = new TrackingCommandExecutor(result(0, "", ""));
        TrackingCommandExecutor unavailableExecutor = new TrackingCommandExecutor(result(1, "", "error"));

        assertTrue(provider(availableExecutor).isAvailable());
        assertFalse(provider(unavailableExecutor).isAvailable());
        assertEquals(List.of("security", "-h"), availableExecutor.calls.getFirst());
    }

    @Test
    void isAvailableRestoresInterruptFlagWhenCommandIsInterrupted() {
        MacKeychainProvider provider = provider((command, stdin) -> {
            throw new InterruptedException("interrupted");
        });

        try {
            assertFalse(provider.isAvailable());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void isAvailableReturnsFalseWhenCommandFails() {
        MacKeychainProvider provider = provider((command, stdin) -> {
            throw new IOException("security command unavailable");
        });

        assertFalse(provider.isAvailable());
    }

    @Test
    void getOrCreateMasterKeyReturnsDecodedExistingKey() throws Exception {
        byte[] existingKey = "existing-secret".getBytes(StandardCharsets.UTF_8);
        TrackingCommandExecutor executor = new TrackingCommandExecutor(
                result(0, Base64.getEncoder().encodeToString(existingKey) + "\n", "")
        );

        byte[] key = provider(executor).getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertArrayEquals(existingKey, key);
        assertEquals(List.of("security", "find-generic-password", "-s", "BeyondSQLExecutor",
                "-a", "master-key", "-w"), executor.calls.getFirst());
    }

    @Test
    void getOrCreateMasterKeyReturnsNullWhenKeyIsMissingAndCreationIsNotAllowed() throws Exception {
        TrackingCommandExecutor executor = new TrackingCommandExecutor(result(1, "", "not found"));

        byte[] key = provider(executor).getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertNull(key);
        assertEquals(1, executor.calls.size());
    }

    @Test
    void getOrCreateMasterKeyTreatsBlankSuccessfulLookupAsMissing() throws Exception {
        TrackingCommandExecutor executor = new TrackingCommandExecutor(result(0, "  \n", ""));

        byte[] key = provider(executor).getOrCreateMasterKey(new SecureMetadata(), null, false);

        assertNull(key);
    }

    @Test
    void getOrCreateMasterKeyCreatesAndStoresKeyWithConfiguredIdentity() throws Exception {
        TrackingCommandExecutor executor = new TrackingCommandExecutor(
                result(1, "", "not found"),
                result(0, "", "")
        );
        SecureMetadata metadata = new SecureMetadata();
        OsProviderConfig config = new OsProviderConfig();
        config.setServiceName("custom-service");
        config.setAccountName("custom-account");
        metadata.setOsProvider(config);

        byte[] key = provider(executor).getOrCreateMasterKey(metadata, null, true);

        assertEquals(32, key.length);
        assertEquals(2, executor.calls.size());
        List<String> storeCommand = executor.calls.get(1);
        assertEquals(List.of("security", "add-generic-password", "-s", "custom-service",
                "-a", "custom-account", "-w", Base64.getEncoder().encodeToString(key), "-U"), storeCommand);
    }

    @Test
    void getOrCreateMasterKeyReportsStoreFailure() {
        TrackingCommandExecutor executor = new TrackingCommandExecutor(
                result(1, "", "not found"),
                result(7, "", "permission denied")
        );

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider(executor).getOrCreateMasterKey(new SecureMetadata(), null, true));

        assertEquals("No se pudo guardar la clave en Keychain: permission denied", exception.getMessage());
    }

    @Test
    void getOrCreateMasterKeyRestoresInterruptFlagAndWrapsInterruption() {
        MacKeychainProvider provider = provider((command, stdin) -> {
            throw new InterruptedException("interrupted");
        });

        try {
            SecureStorageException exception = assertThrows(SecureStorageException.class,
                    () -> provider.getOrCreateMasterKey(new SecureMetadata(), null, false));

            assertEquals("Acceso a Keychain interrumpido.", exception.getMessage());
            assertInstanceOf(InterruptedException.class, exception.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void getOrCreateMasterKeyWrapsUnexpectedErrors() {
        MacKeychainProvider provider = provider((command, stdin) -> {
            throw new IOException("boom");
        });

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider.getOrCreateMasterKey(new SecureMetadata(), null, false));

        assertEquals("No se pudo acceder a Keychain.", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    private static final class TrackingCommandExecutor implements MacKeychainProvider.CommandExecutor {
        private final List<CommandRunner.CommandResult> results;
        private final List<List<String>> calls = new ArrayList<>();
        private int index;

        private TrackingCommandExecutor(CommandRunner.CommandResult... results) {
            this.results = List.of(results);
        }

        @Override
        public CommandRunner.CommandResult run(List<String> command, byte[] stdin) {
            calls.add(command);
            return results.get(index++);
        }
    }
}
