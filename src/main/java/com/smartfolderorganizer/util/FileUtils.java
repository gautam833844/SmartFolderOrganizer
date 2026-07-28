package com.smartfolderorganizer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class providing static file manipulation, inspection, and path resolution capabilities.
 */
public final class FileUtils {

    private FileUtils() {
        throw new UnsupportedOperationException("Utility class 'FileUtils' cannot be instantiated");
    }

    /**
     * Extracts the file extension (lowercase, without leading dot) from a Path.
     *
     * @param path target file path (non-null)
     * @return extension string or empty string if no extension exists
     */
    public static String getExtension(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            return "";
        }
        return getExtension(fileNamePath.toString());
    }

    /**
     * Extracts the file extension (lowercase, without leading dot) from a filename string.
     *
     * @param fileName file name string (non-null)
     * @return extension string or empty string if no extension exists
     */
    public static String getExtension(String fileName) {
        Objects.requireNonNull(fileName, "fileName must not be null");
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase().trim();
        }
        return "";
    }

    /**
     * Gets the base filename without extension from a Path.
     *
     * @param path target file path (non-null)
     * @return base name without extension
     */
    public static String getNameWithoutExtension(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            return "";
        }
        return getNameWithoutExtension(fileNamePath.toString());
    }

    /**
     * Gets the base filename without extension from a filename string.
     *
     * @param fileName file name string (non-null)
     * @return base name without extension
     */
    public static String getNameWithoutExtension(String fileName) {
        Objects.requireNonNull(fileName, "fileName must not be null");
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * Formats bytes into human-readable string. Delegated to {@link SizeFormatter}.
     *
     * @param bytes byte size
     * @return formatted size string
     */
    public static String formatSize(long bytes) {
        return SizeFormatter.formatDecimal(bytes);
    }

    /**
     * Checks if a regular file exists at the specified path.
     *
     * @param path target path
     * @return true if exists and is regular file
     */
    public static boolean exists(Path path) {
        return path != null && Files.exists(path, LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(path);
    }

    /**
     * Checks if a directory exists at the specified path.
     *
     * @param path target directory path
     * @return true if exists and is a directory
     */
    public static boolean isDirectoryExists(Path path) {
        return path != null && Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * Checks if the file or directory at path is hidden.
     *
     * @param path target path (non-null)
     * @return true if hidden or starts with a dot
     */
    public static boolean isHidden(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        try {
            if (Files.isHidden(path)) {
                return true;
            }
        } catch (IOException ignored) {
            // Fallback check for Unix/Windows dot prefix
        }
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().startsWith(".");
    }

    /**
     * Creates directory at path if it does not already exist.
     *
     * @param dir target directory path (non-null)
     * @return the created or existing directory Path
     */
    public static Path createDirectoryIfNotExists(Path dir) {
        Objects.requireNonNull(dir, "directory path must not be null");
        if (!Files.exists(dir)) {
            try {
                return Files.createDirectories(dir);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory: " + dir, e);
            }
        }
        return dir;
    }

    /**
     * Generates a duplicate-safe target Path by appending counter suffix (1), (2) if target exists.
     * <p>
     * Example: {@code photo.jpg} &rarr; {@code photo (1).jpg} &rarr; {@code photo (2).jpg}
     * </p>
     *
     * @param targetPath desired target path (non-null)
     * @return unique target Path that does not currently exist on disk
     */
    public static Path generateUniquePath(Path targetPath) {
        Objects.requireNonNull(targetPath, "targetPath must not be null");
        if (!Files.exists(targetPath)) {
            return targetPath;
        }

        Path parent = targetPath.getParent();
        String fileName = targetPath.getFileName().toString();
        String baseName = getNameWithoutExtension(fileName);
        String extension = getExtension(fileName);
        String extSuffix = extension.isEmpty() ? "" : "." + extension;

        int counter = 1;
        Path candidate;
        do {
            String newName = String.format("%s (%d)%s", baseName, counter++, extSuffix);
            candidate = parent != null ? parent.resolve(newName) : Path.of(newName);
        } while (Files.exists(candidate));

        return candidate;
    }

    /**
     * Generates a duplicate-safe filename string inside a parent directory.
     *
     * @param parentDir target parent directory (non-null)
     * @param fileName  desired filename (non-null)
     * @return unique Path inside parentDir
     */
    public static Path generateUniqueFileName(Path parentDir, String fileName) {
        Objects.requireNonNull(parentDir, "parentDir must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Path targetPath = parentDir.resolve(fileName);
        return generateUniquePath(targetPath);
    }
}
