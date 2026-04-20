package com.zenyrahr.hrms.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class FieldCrypto {
    private static final String PREFIX = "enc:v1:";
    private static final String DEFAULT_DEV_KEY = "change-this-dev-only-key";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final KeyResolution KEY_RESOLUTION = resolveKeyInternal();
    private static final SecretKeySpec SECRET_KEY = new SecretKeySpec(KEY_RESOLUTION.key(), "AES");

    private FieldCrypto() {}

    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (plainText.isBlank() || plainText.startsWith(PREFIX)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt sensitive field", ex);
        }
    }

    public static String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank() || !storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            String payload = storedValue.substring(PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(payload);
            if (decoded.length <= IV_LENGTH) {
                throw new IllegalStateException("Encrypted payload is invalid");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt sensitive field", ex);
        }
    }

    public static boolean isUsingDefaultDevKey() {
        return KEY_RESOLUTION.usingDefaultDevKey();
    }

    public static String keySource() {
        return KEY_RESOLUTION.source();
    }

    private static KeyResolution resolveKeyInternal() {
        String configured = System.getenv("APP_DATA_ENCRYPTION_KEY");
        String source = "env:APP_DATA_ENCRYPTION_KEY";
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("app.data.encryption.key");
            source = "system-property:app.data.encryption.key";
        }
        if (configured == null || configured.isBlank()) {
            configured = LocalEnvFile.get("APP_DATA_ENCRYPTION_KEY");
            source = "file:.env (APP_DATA_ENCRYPTION_KEY)";
        }
        if (configured == null || configured.isBlank()) {
            configured = DEFAULT_DEV_KEY;
            source = "default-dev-key";
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(configured);
            if (bytes.length == 32) {
                return new KeyResolution(bytes, DEFAULT_DEV_KEY.equals(configured), source + " (base64)");
            }
        } catch (IllegalArgumentException ignored) {
            // Not base64; use passphrase derivation below.
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new KeyResolution(
                    digest.digest(configured.getBytes(StandardCharsets.UTF_8)),
                    DEFAULT_DEV_KEY.equals(configured),
                    source + " (sha256-derived)"
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to resolve encryption key", ex);
        }
    }

    private record KeyResolution(byte[] key, boolean usingDefaultDevKey, String source) {}
}
