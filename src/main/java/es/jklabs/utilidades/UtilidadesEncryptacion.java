package es.jklabs.utilidades;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class UtilidadesEncryptacion {

    private static final String INIT_VECTOR = "43&pH#6A8H*w4zLN";
    private static final String KEY = "Y6+RcNdb&&9Wf!V9";
    private static final String VERSION_PREFIX = "v2:";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_LENGTH_BITS = 128;
    private static final int PBKDF2_SALT_LENGTH_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UtilidadesEncryptacion() {

    }

    public static String encrypt(String value) throws GeneralSecurityException {
        if (value == null) {
            return null;
        }
        byte[] salt = new byte[PBKDF2_SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        SecretKey secretKey = deriveKey(salt);
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return VERSION_PREFIX +
                Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(iv) + ":" +
                Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        if (encrypted.startsWith(VERSION_PREFIX)) {
            return decryptV2(encrypted);
        }
        return decryptLegacy(encrypted);
    }

    private static String decryptV2(String encrypted) {
        try {
            String[] parts = encrypted.substring(VERSION_PREFIX.length()).split(":");
            if (parts.length != 3) {
                return null;
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] payload = Base64.getDecoder().decode(parts[2]);
            SecretKey secretKey = deriveKey(salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] original = cipher.doFinal(payload);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Logger.error("desencriptar.dato", ex);
        }
        return null;
    }

    private static String decryptLegacy(String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = cipher.doFinal(DatatypeConverter.parseBase64Binary(encrypted));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Logger.error("desencriptar.dato", ex);
        }
        return null;
    }

    private static SecretKey deriveKey(byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(KEY.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
