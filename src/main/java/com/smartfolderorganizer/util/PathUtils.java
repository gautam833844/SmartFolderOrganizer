package com.smartfolderorganizer.util;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Cross-platform utility class for path validation, normalization, and security checks.
 */
public final class PathUtils {

    private PathUtils() {
        throw new UnsupportedOperationException("Utility class 'PathUtils' cannot be instantiated");
    }

    /**
     * Normalizes path by resolving redundant elements, relative segments, and symbolic aliases.
     *
     * @param path path to normalize (non-null)
     * @return normalized absolute path
     */
    public static Path normalize(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        return path.toAbsolutePath().normalize();
    }

    /**
     * Checks if a string represents a valid syntax Path on the current operating system.
     *
     * @param pathStr path string to validate
     * @return true if valid Path string
     */
    public static boolean isValid(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            return false;
        }
        try {
            Paths.get(pathStr);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * Checks if a Path is readable by the application process.
     *
     * @param path path to check
     * @return true if exists and readable
     */
    public static boolean isReadable(Path path) {
        return path != null && Files.exists(path) && Files.isReadable(path);
    }

    /**
     * Checks if a Path is writable by the application process.
     *
     * @param path path to check
     * @return true if writable
     */
    public static boolean isWritable(Path path) {
        if (path == null) return false;
        if (Files.exists(path)) {
            return Files.isWritable(path);
        }
        Path parent = path.getParent();
        return parent != null && Files.exists(parent) && Files.isWritable(parent);
    }

    /**
     * Validates that a path exists and is readable, throwing an exception if invalid.
     *
     * @param path path to validate
     * @return normalized path if valid
     */
    public static Path validateReadablePath(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        Path norm = normalize(path);
        if (!Files.exists(norm)) {
            throw new IllegalArgumentException("Path does not exist: " + norm);
        }
        if (!Files.isReadable(norm)) {
            throw new SecurityException("Path is not readable: " + norm);
        }
        return norm;
    }

    /**
     * Validates that a path or its parent directory is writable.
     *
     * @param path path to validate
     * @return normalized path if valid
     */
    public static Path validateWritablePath(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        Path norm = normalize(path);
        if (!isWritable(norm)) {
            throw new SecurityException("Path is not writable: " + norm);
        }
        return norm;
    }

    /**
     * Checks if a child path is located inside a parent directory.
     *
     * @param parent directory path (non-null)
     * @param child  target path (non-null)
     * @return true if child is strictly inside parent
     */
    public static boolean isChildOf(Path parent, Path child) {
        Objects.requireNonNull(parent, "parent path must not be null");
        Objects.requireNonNull(child, "child path must not be null");
        Path normParent = normalize(parent);
        Path normChild = normalize(child);
        return normChild.startsWith(normParent) && !normChild.equals(normParent);
    }

    /**
     * Checks if child path is identical to or located inside parent directory.
     *
     * @param parent directory path (non-null)
     * @param child  target path (non-null)
     * @return true if child is same or inside parent
     */
    public static boolean isSameOrChildOf(Path parent, Path child) {
        Objects.requireNonNull(parent, "parent path must not be null");
        Objects.requireNonNull(child, "child path must not be null");
        Path normParent = normalize(parent);
        Path normChild = normalize(child);
        return normChild.startsWith(normParent);
    }

    /**
     * Normalizes file path separators for cross-platform string representations (standardizes to '/').
     *
     * @param pathStr raw path string
     * @return normalized separator string
     */
    public static String normalizeSeparator(String pathStr) {
        if (pathStr == null) return null;
        return pathStr.replace('\\', '/');
    }

    /**
     * Safely parses a string into a Path object.
     *
     * @param pathStr raw path string
     * @return Path instance
     */
    public static Path toPath(String pathStr) {
        Objects.requireNonNull(pathStr, "pathStr must not be null");
        return Path.of(pathStr);
    }
}
