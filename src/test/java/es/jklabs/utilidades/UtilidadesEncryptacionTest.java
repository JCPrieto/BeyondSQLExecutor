package es.jklabs.utilidades;

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilidadesEncryptacionTest {

    @Test
    void encryptPortableCompatIncludesRandomIvAndCanDecrypt() throws Exception {
        String encrypted1 = UtilidadesEncryptacion.encryptPortableCompat("secret");
        String encrypted2 = UtilidadesEncryptacion.encryptPortableCompat("secret");

        assertTrue(encrypted1.startsWith("v1:"));
        assertTrue(encrypted2.startsWith("v1:"));
        assertEquals("secret", UtilidadesEncryptacion.decrypt(encrypted1));
        assertEquals("secret", UtilidadesEncryptacion.decrypt(encrypted2));
        org.junit.jupiter.api.Assertions.assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decryptLegacySupportsPreviousStaticIvFormat() throws GeneralSecurityException {
        String oldFormatCiphertext = "WjKBaUQiG4FV0M3dxQ6bNA==";

        assertEquals("secret", UtilidadesEncryptacion.decrypt(oldFormatCiphertext));
    }
}
