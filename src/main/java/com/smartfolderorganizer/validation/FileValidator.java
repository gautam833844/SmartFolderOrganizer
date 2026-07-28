package com.smartfolderorganizer.validation;

import com.smartfolderorganizer.exception.ValidationException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Production-quality validator for file names, extensions, sizes, invalid characters, and OS reserved names.
 */
public final class FileValidator {

    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
            "CLOCK$"
    );

    // Characters forbidden across Windows/POSIX file systems
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[\\x00-\\x1F\\x7F<>:\"/\\\\|?*]");

    private FileValidator() {
        throw new UnsupportedOperationException("Utility class 'FileValidator' cannot be instantiated");
    }

    /**
     * Validates that a filename string is non-null, non-blank, free of illegal characters and OS reserved names.
     *
     * @param fileName filename string to validate
     */
    public static void validateFileName(String fileName) {
        Objects.requireNonNull(fileName, "fileName must not be null");
        if (fileName.isBlank()) {
            throw new ValidationException("Filename cannot be empty or blank");
        }
        if (hasInvalidCharacters(fileName)) {
            throw new ValidationException("Filename contains illegal characters: " + fileName);
        }
        if (isWindowsReservedName(fileName)) {
            throw new ValidationException("Filename is a system reserved name: " + fileName);
        }
    }

    /**
     * Validates file extension format.
     *
     * @param extension file extension string
     */
    public static void validateExtension(String extension) {
        Objects.requireNonNull(extension, "extension must not be null");
        if (hasInvalidCharacters(extension)) {
            throw new ValidationException("Extension contains invalid characters: " + extension);
        }
    }

    /**
     * Validates that file size is non-negative and within acceptable application bounds.
     *
     * @param sizeBytes    file size in bytes
     * @param maxSizeBytes maximum permitted size in bytes (must be > 0)
     */
    public static void validateFileSize(long sizeBytes, long maxSizeBytes) {
        if (sizeBytes < 0) {
            throw new ValidationException("File size cannot be negative: " + sizeBytes);
        }
        if (maxSizeBytes > 0 && sizeBytes > maxSizeBytes) {
            throw new ValidationException(String.format("File size (%d bytes) exceeds maximum limit (%d bytes)", sizeBytes, maxSizeBytes));
        }
    }

    /**
     * Checks if a filename contains illegal characters (<, >, :, ", /, \, |, ?, *, control chars).
     *
     * @param fileName file name string
     * @return true if illegal characters exist
     */
    public static boolean hasInvalidCharacters(String fileName) {
        if (fileName == null) return false;
        return INVALID_CHARS_PATTERN.matcher(fileName).find();
    }

    /**
     * Checks if a filename is a reserved Windows device name (e.g. CON, PRN, AUX, NUL, COM1, LPT1).
     *
     * @param fileName file name string
     * @return true if reserved name
     */
    public static boolean isWindowsReservedName(String fileName) {
        if (fileName == null || fileName.isBlank()) return false;
        String baseName = fileName.trim();
        int dotIdx = baseName.indexOf('.');
        if (dotIdx > 0) {
            baseName = baseName.substring(0, dotIdx);
        }
        return WINDOWS_RESERVED_NAMES.contains(baseName.toUpperCase(Locale.US));
    }
}
