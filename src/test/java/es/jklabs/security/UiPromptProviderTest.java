package es.jklabs.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class UiPromptProviderTest {
    private static final String REQUIRED_PASSWORD_MESSAGE = "No se ha indicado la contraseña maestra.";

    private static SecureMetadata metadataWithKdfParams() {
        UiKdfParams params = new UiKdfParams();
        params.setSaltB64(Base64.getEncoder().encodeToString(
                "1234567890abcdef".getBytes(StandardCharsets.UTF_8)));
        params.setIterations(1000);
        params.setAlgorithm("PBKDF2WithHmacSHA256");
        SecureMetadata metadata = new SecureMetadata();
        metadata.setUiKdfParams(params);
        return metadata;
    }

    @Test
    void exposesProviderProperties() {
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> null);

        assertEquals(UiPromptProvider.ID, provider.getId());
        assertEquals("Password Prompt", provider.getDisplayName());
        assertEquals(10, provider.getDefaultPriority());
        assertTrue(provider.isAvailable());
    }

    @Test
    void returnsNullWithoutPromptWhenMetadataIsMissingAndCreationIsNotAllowed() throws Exception {
        boolean[] prompted = {false};
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> {
            prompted[0] = true;
            return "unused".toCharArray();
        });

        assertNull(provider.getOrCreateMasterKey(new SecureMetadata(), null, false));
        assertFalse(prompted[0]);
    }

    @Test
    void rejectsCancelledCreation() {
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> null);

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider.getOrCreateMasterKey(new SecureMetadata(), null, true));

        assertEquals(REQUIRED_PASSWORD_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsEmptyPasswordDuringCreationAndWipesIt() {
        char[] password = new char[0];
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> password);

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider.getOrCreateMasterKey(new SecureMetadata(), null, true));

        assertEquals(REQUIRED_PASSWORD_MESSAGE, exception.getMessage());
        assertArrayEquals(new char[0], password);
    }

    @Test
    void createsKeyAndKdfMetadataAndWipesPassword() throws Exception {
        char[] password = "master-password".toCharArray();
        SecureMetadata metadata = new SecureMetadata();
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> password);

        byte[] key = provider.getOrCreateMasterKey(metadata, null, true);

        assertEquals(32, key.length);
        assertNotNull(metadata.getUiKdfParams());
        assertEquals(120000, metadata.getUiKdfParams().getIterations());
        assertEquals("PBKDF2WithHmacSHA256", metadata.getUiKdfParams().getAlgorithm());
        assertEquals(16, Base64.getDecoder().decode(metadata.getUiKdfParams().getSaltB64()).length);
        assertArrayEquals(new char[password.length], password);
    }

    @Test
    void rejectsCancelledExistingPasswordPrompt() {
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> null);

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider.getOrCreateMasterKey(metadataWithKdfParams(), null, false));

        assertEquals(REQUIRED_PASSWORD_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsEmptyExistingPassword() {
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> new char[0]);

        SecureStorageException exception = assertThrows(SecureStorageException.class,
                () -> provider.getOrCreateMasterKey(metadataWithKdfParams(), null, false));

        assertEquals(REQUIRED_PASSWORD_MESSAGE, exception.getMessage());
    }

    @Test
    void derivesExistingKeyAndWipesPassword() throws Exception {
        char[] password = "master-password".toCharArray();
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> password);
        SecureMetadata metadata = metadataWithKdfParams();

        byte[] key = provider.getOrCreateMasterKey(metadata, null, false);
        byte[] expected = CryptoUtils.deriveKeyPbkdf2("master-password".toCharArray(),
                "1234567890abcdef".getBytes(StandardCharsets.UTF_8), 1000);

        assertArrayEquals(expected, key);
        assertArrayEquals(new char[password.length], password);
    }

    @Test
    void resetRemovesKdfMetadata() throws Exception {
        SecureMetadata metadata = metadataWithKdfParams();
        UiPromptProvider provider = new UiPromptProvider((parent, title) -> null);

        provider.reset(metadata, null);

        assertNull(metadata.getUiKdfParams());
    }
}
