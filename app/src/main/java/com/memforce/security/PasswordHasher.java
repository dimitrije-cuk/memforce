package com.memforce.security;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Derives and verifies salted PBKDF2 password hashes. */
public final class PasswordHasher {

    // PBKDF2WithHmacSHA256 is only guaranteed from API 26, so the SHA1 variant is used instead.
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int BASE64_FLAGS = Base64.NO_WRAP;

    private PasswordHasher() {
    }

    @NonNull
    public static String newSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, BASE64_FLAGS);
    }

    @NonNull
    public static String hash(@NonNull String password, @NonNull String salt) {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), Base64.decode(salt, BASE64_FLAGS), ITERATIONS, KEY_LENGTH_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            return Base64.encodeToString(key, BASE64_FLAGS);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 is unavailable on this device", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean matches(@NonNull String password, @NonNull String salt, @NonNull String expectedHash) {
        return MessageDigest.isEqual(
                Base64.decode(hash(password, salt), BASE64_FLAGS),
                Base64.decode(expectedHash, BASE64_FLAGS));
    }
}
