package es.jklabs.utilidades;

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

public class UtilidadesEncryptacionTest {

    @Test
    void encryptPortableCompatUsesAuthenticatedV2FormatAndCanDecrypt() throws Exception {
        String encrypted1 = UtilidadesEncryptacion.encryptPortableCompat("secret");
        String encrypted2 = UtilidadesEncryptacion.encryptPortableCompat("secret");

        assertTrue(encrypted1.startsWith("v2:"));
        assertTrue(encrypted2.startsWith("v2:"));
        assertEquals("secret", UtilidadesEncryptacion.decrypt(encrypted1));
        assertEquals("secret", UtilidadesEncryptacion.decrypt(encrypted2));
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decryptLegacySupportsPreviousStaticIvFormat() throws GeneralSecurityException {
        String oldFormatCiphertext = "WjKBaUQiG4FV0M3dxQ6bNA==";

        assertEquals("secret", UtilidadesEncryptacion.decrypt(oldFormatCiphertext));
    }

    @Test
    void decryptLegacySupportsV1IvAndCiphertextFormat() {
        String v1Ciphertext = "v1:NDMmcEgjNkE4SCp3NHpMTg==:WjKBaUQiG4FV0M3dxQ6bNA==";

        assertEquals("secret", UtilidadesEncryptacion.decrypt(v1Ciphertext));
    }
}
