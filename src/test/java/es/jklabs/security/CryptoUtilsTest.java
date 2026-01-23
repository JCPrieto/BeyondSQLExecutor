package es.jklabs.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CryptoUtilsTest {

    @Test
    void aesGcmRoundTripWithAad() throws GeneralSecurityException {
        byte[] key = CryptoUtils.randomBytes(32);
        byte[] aad = "cred:test".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = "secret".getBytes(StandardCharsets.UTF_8);
        CryptoUtils.AesGcmPayload payload = CryptoUtils.encryptAesGcm(key, aad, plaintext);
        byte[] decrypted = CryptoUtils.decryptAesGcm(key, aad, payload.nonce(), payload.ciphertext());
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void pbkdf2DerivationIsStable() throws GeneralSecurityException {
        char[] password = "master".toCharArray();
        byte[] salt = "1234567890abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] key1 = CryptoUtils.deriveKeyPbkdf2(password, salt, 1000);
        byte[] key2 = CryptoUtils.deriveKeyPbkdf2(password, salt, 1000);
        assertEquals(32, key1.length);
        assertEquals(32, key2.length);
        assertArrayEquals(key1, key2);
        Arrays.fill(password, '\0');
    }
}
