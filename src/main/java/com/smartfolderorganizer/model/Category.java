package com.smartfolderorganizer.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Domain Enum representing logical file categories supported by the Smart Folder Organizer.
 * <p>
 * Each category defines a human-readable display name, target folder name, and a set of
 * recognized file extensions.
 * </p>
 */
public enum Category {

    IMAGES("Images", "Images", Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "tif", "ico", "heic", "heif", "raw", "psd"
    )),

    VIDEOS("Videos", "Videos", Set.of(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "mpeg", "mpg", "vob"
    )),

    DOCUMENTS("Documents", "Documents", Set.of(
            "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv", "md", "epub", "pages", "numbers", "key"
    )),

    PDF("PDF Documents", "PDFs", Set.of(
            "pdf"
    )),

    AUDIO("Audio", "Audio", Set.of(
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "aiff", "mid", "midi", "alac"
    )),

    ARCHIVES("Archives", "Archives", Set.of(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "tgz", "cab"
    )),

    CODE("Source Code", "Code", Set.of(
            "java", "py", "js", "ts", "html", "css", "cpp", "c", "h", "cs", "go", "rs", "php", "rb", "sql",
            "json", "xml", "yaml", "yml", "sh", "bat", "ps1", "kt", "swift"
    )),

    EXECUTABLES("Executables", "Executables", Set.of(
            "exe", "msi", "cmd", "bin", "app", "dmg", "deb", "rpm", "jar", "com"
    )),

    FONTS("Fonts", "Fonts", Set.of(
            "ttf", "otf", "woff", "woff2", "eot"
    )),

    OTHERS("Others", "Others", Collections.emptySet());

    private final String displayName;
    private final String folderName;
    private final Set<String> supportedExtensions;

    /**
     * Constructs a Category enum instance.
     *
     * @param displayName         human-readable display name (non-null)
     * @param folderName          destination directory name (non-null)
     * @param supportedExtensions set of supported extensions without leading dots (non-null)
     */
    Category(String displayName, String folderName, Set<String> supportedExtensions) {
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.folderName = Objects.requireNonNull(folderName, "folderName must not be null");
        Objects.requireNonNull(supportedExtensions, "supportedExtensions must not be null");

        Set<String> cleanSet = new HashSet<>();
        for (String ext : supportedExtensions) {
            if (ext != null && !ext.isBlank()) {
                cleanSet.add(ext.toLowerCase().trim().replaceFirst("^\\.", ""));
            }
        }
        this.supportedExtensions = Collections.unmodifiableSet(cleanSet);
    }

    /**
     * Checks whether this category supports the given file extension.
     *
     * @param extension the file extension to test (e.g. "png" or ".png")
     * @return true if extension matches this category, false otherwise
     */
    public boolean supportsExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        String cleanExt = extension.toLowerCase().trim().replaceFirst("^\\.", "");
        return supportedExtensions.contains(cleanExt);
    }

    /**
     * Returns the human-readable display name.
     *
     * @return display name string
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the folder name used for organization.
     *
     * @return folder name string
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * Returns the unmodifiable set of supported file extensions.
     *
     * @return set of lowercase file extension strings
     */
    public Set<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    /**
     * Resolves a Category based on a file extension.
     *
     * @param extension file extension (e.g., "pdf", ".jpg")
     * @return matching Category or {@link Category#OTHERS} if not matched
     */
    public static Category fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return OTHERS;
        }
        String cleanExt = extension.toLowerCase().trim().replaceFirst("^\\.", "");
        for (Category category : values()) {
            if (category != OTHERS && category.supportsExtension(cleanExt)) {
                return category;
            }
        }
        return OTHERS;
    }
}
