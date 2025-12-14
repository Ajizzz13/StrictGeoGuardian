package com.zeroends.strictgeoguardian.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class HashUtils {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public static String hmacSha256(String key, String data) {
        if (data == null || key == null) {
            return "null_data";
        }
        try {
            Mac sha256Hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }

    public static String sha256(String data) {
        if (data == null) return "null_data";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate SHA-256", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static String getSubnetPrefix(InetAddress address, int prefixLength) {
        byte[] ipBytes = address.getAddress();
        int numBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;

        byte[] subnetBytes = new byte[ipBytes.length];
        
        for (int i = 0; i < numBytes; i++) {
            subnetBytes[i] = ipBytes[i];
        }

        if (remainingBits > 0) {
            byte mask = (byte) (0xFF << (8 - remainingBits));
            subnetBytes[numBytes] = (byte) (ipBytes[numBytes] & mask);
        }
        
        StringBuilder sb = new StringBuilder();
        if (address instanceof Inet4Address) {
            for (int i = 0; i < 4; i++) {
                sb.append(subnetBytes[i] & 0xFF);
                if (i < 3) sb.append('.');
            }
        } else {
             for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", subnetBytes[i]));
                if (i % 2 == 1 && i < 15) sb.append(':');
            }
        }
        sb.append("/").append(prefixLength);
        return sb.toString();
    }
}
