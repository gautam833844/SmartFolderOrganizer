package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe utility for probing file MIME types and resolving corresponding domain {@link Category} instances.
 * <p>
 * Guarantees exception-safe probing using {@link Files#probeContentType(Path)} and MIME prefix mappings.
 * </p>
 */
public final class MimeTypeDetector {

    private static final Map<String, Category> MIME_CATEGORY_MAP = new ConcurrentHashMap<>();

    static {
        // Seed default MIME type mappings
        registerMimeCategory("image/", Category.IMAGES);
        registerMimeCategory("video/", Category.VIDEOS);
        registerMimeCategory("audio/", Category.AUDIO);
        registerMimeCategory("font/", Category.FONTS);
        registerMimeCategory("text/", Category.DOCUMENTS);

        // Specific application types
        registerMimeCategory("application/pdf", Category.PDF);
        registerMimeCategory("application/zip", Category.ARCHIVES);
        registerMimeCategory("application/x-rar-compressed", Category.ARCHIVES);
        registerMimeCategory("application/x-7z-compressed", Category.ARCHIVES);
        registerMimeCategory("application/x-tar", Category.ARCHIVES);
        registerMimeCategory("application/gzip", Category.ARCHIVES);
        registerMimeCategory("application/json", Category.CODE);
        registerMimeCategory("application/xml", Category.CODE);
        registerMimeCategory("application/javascript", Category.CODE);
        registerMimeCategory("application/x-msdownload", Category.EXECUTABLES);
        registerMimeCategory("application/x-executable", Category.EXECUTABLES);
        registerMimeCategory("application/msword", Category.DOCUMENTS);
        registerMimeCategory("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Category.DOCUMENTS);
        registerMimeCategory("application/vnd.ms-excel", Category.DOCUMENTS);
        registerMimeCategory("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Category.DOCUMENTS);
    }

    private MimeTypeDetector() {
        throw new UnsupportedOperationException("Utility class 'MimeTypeDetector' cannot be instantiated");
    }

    /**
     * Registers a custom MIME type or prefix mapping to a Category.
     *
     * @param mimeTypeOrPrefix MIME type (e.g. "application/pdf") or prefix (e.g. "image/")
     * @param category         target domain Category
     */
    public static void registerMimeCategory(String mimeTypeOrPrefix, Category category) {
        Objects.requireNonNull(mimeTypeOrPrefix, "mimeTypeOrPrefix must not be null");
        Objects.requireNonNull(category, "category must not be null");
        MIME_CATEGORY_MAP.put(mimeTypeOrPrefix.toLowerCase().trim(), category);
    }

    /**
     * Probes the MIME content type of a file at the specified path without throwing exceptions.
     *
     * @param path target file path
     * @return probed MIME type string or "application/octet-stream" if probing fails
     */
    public static String detectMimeType(Path path) {
        if (path == null || !Files.exists(path)) {
            return "application/octet-stream";
        }
        try {
            String probed = Files.probeContentType(path);
            if (probed != null && !probed.isBlank()) {
                return probed.toLowerCase().trim();
            }
        } catch (IOException ignored) {
            // Graceful fallback on IO / OS probe failure
        }
        return "application/octet-stream";
    }

    /**
     * Maps a MIME type string to a matching domain Category.
     *
     * @param mimeType MIME type string (e.g. "image/png", "application/pdf")
     * @return matching Category or {@link Category#OTHERS}
     */
    public static Category detectCategoryFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return Category.OTHERS;
        }

        String cleanMime = mimeType.toLowerCase().trim();

        // Direct exact match lookup
        if (MIME_CATEGORY_MAP.containsKey(cleanMime)) {
            return MIME_CATEGORY_MAP.get(cleanMime);
        }

        // Prefix lookup (e.g. "image/jpeg" -> matches "image/")
        for (Map.Entry<String, Category> entry : MIME_CATEGORY_MAP.entrySet()) {
            if (entry.getKey().endsWith("/") && cleanMime.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return Category.OTHERS;
    }

    /**
     * Probes file at path and attempts to detect Category from MIME type.
     *
     * @param path file path
     * @return Optional containing Category if mapped from MIME, empty otherwise
     */
    public static Optional<Category> detectCategory(Path path) {
        String mime = detectMimeType(path);
        Category category = detectCategoryFromMimeType(mime);
        return category != Category.OTHERS ? Optional.of(category) : Optional.empty();
    }
}
