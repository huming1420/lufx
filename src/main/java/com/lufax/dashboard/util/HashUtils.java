package com.lufax.dashboard.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class HashUtils {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String sha256(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String sha1(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public String computeSha256(String input) {
        return sha256(input);
    }

    public String computeSha1(String input) {
        return sha1(input);
    }

    public static String computeMetricVersion(String data) {
        if (data == null) {
            return "unknown";
        }
        String sortedJson = JsonUtils.toSortedJson(data);
        String hash = sha1(sortedJson);
        return hash.length() >= 8 ? hash.substring(0, 8) : hash;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}