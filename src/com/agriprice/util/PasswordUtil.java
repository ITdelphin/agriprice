package com.agriprice.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility class for secure salted password hashing using PBKDF2WithHmacSHA256.
 * Implements industry standard iterations, random salt generation, and constant-time verification.
 */
public class PasswordUtil {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_BYTES = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Generates a cryptographically secure random salt in hexadecimal format.
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Hashes a password with a given hex salt using PBKDF2.
     */
    public static String hashPassword(String password, String saltHex) {
        if (password == null || saltHex == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        return hashPassword(password.toCharArray(), saltHex);
    }

    /**
     * Hashes a char array password with a given hex salt using PBKDF2.
     */
    public static String hashPassword(char[] password, String saltHex) {
        byte[] salt = hexToBytes(saltHex);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password with PBKDF2", e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Verifies whether an input password matches the stored salt and hash.
     * Uses constant-time comparison to prevent timing attacks.
     */
    public static boolean verifyPassword(String password, String saltHex, String expectedHashHex) {
        if (password == null || saltHex == null || expectedHashHex == null) {
            return false;
        }
        String computedHashHex = hashPassword(password.toCharArray(), saltHex);
        return slowEquals(computedHashHex, expectedHashHex);
    }

    /**
     * Constant-time comparison to mitigate timing attacks.
     */
    private static boolean slowEquals(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        int diff = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            diff |= aBytes[i] ^ bBytes[i];
        }
        return diff == 0;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static void main(String[] args) {
        String testPassword = args.length > 0 ? args[0] : System.getenv("AGRIPRICE_TEST_PASSWORD");
        if (testPassword == null || testPassword.isEmpty()) {
            System.out.println("Provide a password argument or set AGRIPRICE_TEST_PASSWORD.");
            return;
        }
        String salt = generateSalt();
        String hash = hashPassword(testPassword, salt);
        System.out.println("Salt: " + salt);
        System.out.println("Hash: " + hash);
        System.out.println("Verified: " + verifyPassword(testPassword, salt, hash));
    }
}
