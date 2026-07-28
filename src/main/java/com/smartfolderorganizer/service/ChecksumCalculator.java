package com.smartfolderorganizer.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Utility class for computing file checksum hashes using streaming algorithms to minimize memory usage.
 */
public final class ChecksumCalculator {

    private static final int BUFFER_SIZE = 65536; // 64KB buffer for efficient stream reading

    private ChecksumCalculator() {
        throw new UnsupportedOperationException("Utility class 'ChecksumCalculator' cannot be instantiated");
    }

    /**
     * Calculates MD5 checksum for a file.
     *
     * @param path file path (non-null)
     * @return hex encoded MD5 checksum string
     */
    public static String calculateMD5(Path path) {
        return calculate(path, "MD5");
    }

    /**
     * Calculates SHA-1 checksum for a file.
     *
     * @param path file path (non-null)
     * @return hex encoded SHA-1 checksum string
     */
    public static String calculateSHA1(Path path) {
        return calculate(path, "SHA-1");
    }

    /**
     * Calculates SHA-256 checksum for a file.
     *
     * @param path file path (non-null)
     * @return hex encoded SHA-256 checksum string
     */
    public static String calculateSHA256(Path path) {
        return calculate(path, "SHA-256");
    }

    /**
     * Calculates checksum for a file using the specified hashing algorithm (e.g. "MD5", "SHA-1", "SHA-256").
     * <p>
     * Reads the file in chunks using an {@link InputStream} to prevent loading full file contents into memory.
     * </p>
     *
     * @param path      file path (non-null)
     * @param algorithm hashing algorithm name (non-null)
     * @return hex encoded checksum string
     */
    public static String calculate(Path path, String algorithm) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path must point to an existing regular file: " + path);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to calculate %s checksum for file: %s", algorithm, path), e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
