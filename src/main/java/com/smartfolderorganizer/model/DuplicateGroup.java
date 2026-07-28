package com.smartfolderorganizer.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Domain entity representing a group of identical or duplicate files sharing a common hash or checksum.
 * <p>
 * Useful for duplicate detection, identifying redundant files, and calculating total wasted disk space.
 * </p>
 */
public final class DuplicateGroup {

    private final String hash;
    private final List<FileItem> files;
    private final long duplicateSize;

    private DuplicateGroup(Builder builder) {
        this.hash = Objects.requireNonNull(builder.hash, "hash must not be null");
        if (this.hash.isBlank()) {
            throw new IllegalArgumentException("hash must not be blank");
        }
        Objects.requireNonNull(builder.files, "files must not be null");
        if (builder.files.size() < 1) {
            throw new IllegalArgumentException("Duplicate group must contain at least 1 file item");
        }
        this.files = List.copyOf(builder.files);

        long singleFileSize = this.files.get(0).getSize();
        long calculatedWasted = (long) (this.files.size() - 1) * singleFileSize;
        this.duplicateSize = builder.duplicateSize >= 0 ? builder.duplicateSize : Math.max(0, calculatedWasted);
    }

    /**
     * Gets the content hash or checksum identifying this duplicate group.
     *
     * @return non-null hash string
     */
    public String getHash() {
        return hash;
    }

    public String getChecksumHash() {
        return hash;
    }

    /**
     * Gets all file items belonging to this duplicate group.
     *
     * @return unmodifiable list of FileItems
     */
    public List<FileItem> getFiles() {
        return files;
    }

    /**
     * Gets total wasted duplicate size in bytes (excluding 1 original instance).
     *
     * @return size in bytes
     */
    public long getDuplicateSize() {
        return duplicateSize;
    }

    public long getDuplicateBytes() {
        return duplicateSize;
    }

    /**
     * Gets total cumulative size of all files in this group.
     *
     * @return total bytes
     */
    public long getTotalSize() {
        return files.stream().mapToLong(FileItem::getSize).sum();
    }

    public long getTotalBytes() {
        return getTotalSize();
    }

    /**
     * Gets count of files in this group.
     *
     * @return number of duplicate items
     */
    public int getFileCount() {
        return files.size();
    }

    /**
     * Returns the primary or original file item (first file in group).
     *
     * @return Optional containing primary FileItem or empty if list is empty
     */
    public Optional<FileItem> getOriginalFile() {
        return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
    }

    /**
     * Gets all files in this group except the primary original file.
     *
     * @return unmodifiable list of redundant FileItems
     */
    public List<FileItem> getDuplicatesOnly() {
        if (files.size() <= 1) {
            return Collections.emptyList();
        }
        return files.subList(1, files.size());
    }

    /**
     * Formats wasted duplicate size into human-readable string.
     *
     * @return formatted size string
     */
    public String getFormattedDuplicateSize() {
        return formatBytes(duplicateSize);
    }

    /**
     * Static factory method to create a DuplicateGroup from hash and files.
     *
     * @param hash checksum or content hash
     * @param files list of duplicate files
     * @return new DuplicateGroup instance
     */
    public static DuplicateGroup of(String hash, List<FileItem> files) {
        return builder().hash(hash).files(files).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %cB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuplicateGroup that = (DuplicateGroup) o;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public String toString() {
        return "DuplicateGroup{" +
                "hash='" + hash + '\'' +
                ", fileCount=" + getFileCount() +
                ", duplicateSize=" + getFormattedDuplicateSize() +
                '}';
    }

    /**
     * Builder for constructing immutable {@link DuplicateGroup} instances.
     */
    public static final class Builder {
        private String hash;
        private List<FileItem> files = Collections.emptyList();
        private long duplicateSize = -1;

        public Builder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public Builder files(List<FileItem> files) {
            this.files = files != null ? files : Collections.emptyList();
            return this;
        }

        public Builder duplicateSize(long duplicateSize) {
            this.duplicateSize = duplicateSize;
            return this;
        }

        public DuplicateGroup build() {
            return new DuplicateGroup(this);
        }
    }
}
