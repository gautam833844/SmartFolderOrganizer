package com.smartfolderorganizer.validation;

import com.smartfolderorganizer.exception.ValidationException;
import com.smartfolderorganizer.util.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Production-quality utility class for validating file system paths, permissions, directory hierarchy integrity,
 * and security constraints such as directory traversal attacks.
 */
public final class PathValidator {

    private PathValidator() {
        throw new UnsupportedOperationException("Utility class 'PathValidator' cannot be instantiated");
    }

    /**
     * Validates that a path is non-null.
     *
     * @param path target path
     * @param paramName descriptive parameter name for error messages
     */
    public static void validateNotNull(Path path, String paramName) {
        Objects.requireNonNull(paramName, "paramName must not be null");
        if (path == null) {
            throw new ValidationException(paramName + " path cannot be null");
        }
    }

    /**
     * Validates that a path exists on the local file system.
     *
     * @param path target path (non-null)
     */
    public static void validateExists(Path path) {
        validateNotNull(path, "Target");
        if (!Files.exists(path)) {
            throw new ValidationException("Path does not exist: " + path);
        }
    }

    /**
     * Validates that a path exists and is a directory.
     *
     * @param path target path (non-null)
     */
    public static void validateIsDirectory(Path path) {
        validateExists(path);
        if (!Files.isDirectory(path)) {
            throw new ValidationException("Path is not a directory: " + path);
        }
    }

    /**
     * Validates that a path exists and is a regular file.
     *
     * @param path target path (non-null)
     */
    public static void validateIsFile(Path path) {
        validateExists(path);
        if (!Files.isRegularFile(path)) {
            throw new ValidationException("Path is not a regular file: " + path);
        }
    }

    /**
     * Validates that a path exists and has read permissions.
     *
     * @param path target path (non-null)
     */
    public static void validateReadable(Path path) {
        validateExists(path);
        if (!PathUtils.isReadable(path)) {
            throw new ValidationException("Path is not readable: " + path);
        }
    }

    /**
     * Validates that a path or its parent directory has write permissions.
     *
     * @param path target path (non-null)
     */
    public static void validateWritable(Path path) {
        validateNotNull(path, "Target");
        if (!PathUtils.isWritable(path)) {
            throw new ValidationException("Path is not writable: " + path);
        }
    }

    /**
     * Validates that a path is not hidden or system dot file.
     *
     * @param path target path (non-null)
     */
    public static void validateNotHidden(Path path) {
        validateExists(path);
        try {
            if (Files.isHidden(path) || (path.getFileName() != null && path.getFileName().toString().startsWith("."))) {
                throw new ValidationException("Hidden path is not allowed: " + path);
            }
        } catch (Exception e) {
            if (e instanceof ValidationException) throw (ValidationException) e;
            throw new ValidationException("Failed to check if path is hidden: " + path, e);
        }
    }

    /**
     * Validates a source directory for scanning or organization.
     *
     * @param source source directory path
     */
    public static void validateSourceFolder(Path source) {
        validateNotNull(source, "Source directory");
        validateIsDirectory(source);
        validateReadable(source);
    }

    /**
     * Validates a target destination directory.
     *
     * @param destination destination directory path
     */
    public static void validateDestinationFolder(Path destination) {
        validateNotNull(destination, "Destination directory");
        validateWritable(destination);
    }

    /**
     * Validates a readable file item for move/copy operations.
     *
     * @param file target file path
     */
    public static void validateReadableFile(Path file) {
        validateNotNull(file, "Target file");
        validateIsFile(file);
        validateReadable(file);
    }

    /**
     * Validates a writable target directory.
     *
     * @param directory target directory path
     */
    public static void validateWritableDirectory(Path directory) {
        validateNotNull(directory, "Target directory");
        validateIsDirectory(directory);
        validateWritable(directory);
    }

    /**
     * Validates a full organization request comparing source and destination directories.
     * <p>
     * Ensures source and destination exist, are not identical, and destination is not nested inside source
     * or vice-versa to prevent directory traversal and recursive loops.
     * </p>
     *
     * @param source      source directory
     * @param destination destination directory
     */
    public static void validateOrganizationRequest(Path source, Path destination) {
        validateSourceFolder(source);
        validateDestinationFolder(destination);

        Path normSource = PathUtils.normalize(source);
        Path normDest = PathUtils.normalize(destination);

        if (normSource.equals(normDest)) {
            throw new ValidationException("Source and Destination directories cannot be identical: " + normSource);
        }

        if (PathUtils.isChildOf(normSource, normDest)) {
            throw new ValidationException("Destination directory cannot be nested inside source directory: " + normDest);
        }

        if (PathUtils.isChildOf(normDest, normSource)) {
            throw new ValidationException("Source directory cannot be nested inside destination directory: " + normSource);
        }
    }
}
