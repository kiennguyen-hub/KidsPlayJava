package com.kidsplay.app.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Băm mã PIN phụ huynh bằng SHA-256 để không lưu PIN dạng chữ thường trong database. */
public final class PinHasher {

    private static final String SALT = "kidsplay.parent.pin.v1:";

    private PinHasher() {}

    public static String hash(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String storedHash, String enteredPin) {
        return storedHash != null && storedHash.equals(hash(enteredPin));
    }
}
