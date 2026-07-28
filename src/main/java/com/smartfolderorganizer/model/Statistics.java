package com.smartfolderorganizer.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain entity holding comprehensive statistical metrics for a set of scanned or organized files.
 * <p>
 * Completely immutable and offers dynamic statistical computation via factory methods.
 * </p>
 */
public final class Statistics {

    private final long totalFiles;
    private final long totalSize;
    private final FileItem largestFile;
    private final FileItem smallestFile;
    private final Map<Category, Long> categoryCounts;
    private final double averageSize;
    private final long duplicateCount;

    private Statistics(Builder builder) {
        if (builder.totalFiles < 0) {
            throw new IllegalArgumentException("totalFiles cannot be negative");
        }
        if (builder.totalSize < 0) {
            throw new IllegalArgumentException("totalSize cannot be negative");
        }
        if (builder.averageSize < 0) {
            throw new IllegalArgumentException("averageSize cannot be negative");
        }
        if (builder.duplicateCount < 0) {
            throw new IllegalArgumentException("duplicateCount cannot be negative");
        }

        this.totalFiles = builder.totalFiles;
        this.totalSize = builder.totalSize;
        this.largestFile = builder.largestFile;
        this.smallestFile = builder.smallestFile;
        this.averageSize = builder.averageSize;
        this.duplicateCount = builder.duplicateCount;

        Map<Category, Long> cleanCounts = new EnumMap<>(Category.class);
        for (Category cat : Category.values()) {
            cleanCounts.put(cat, 0L);
        }
        if (builder.categoryCounts != null) {
            builder.categoryCounts.forEach((cat, count) -> {
                if (cat != null && count != null) {
                    cleanCounts.put(cat, Math.max(0L, count));
                }
            });
        }
        this.categoryCounts = Collections.unmodifiableMap(cleanCounts);
    }

    /**
     * Gets total file count.
     *
     * @return total files
     */
    public long getTotalFiles() {
        return totalFiles;
    }

    /**
     * Gets aggregate file size in bytes.
     *
     * @return total size in bytes
     */
    public long getTotalSize() {
        return totalSize;
    }

    public long getTotalSizeBytes() {
        return totalSize;
    }

    /**
     * Gets the largest file item if available.
     *
     * @return Optional containing largest FileItem
     */
    public Optional<FileItem> getLargestFile() {
        return Optional.ofNullable(largestFile);
    }

    /**
     * Gets the smallest file item if available.
     *
     * @return Optional containing smallest FileItem
     */
    public Optional<FileItem> getSmallestFile() {
        return Optional.ofNullable(smallestFile);
    }

    /**
     * Gets category distribution counts.
     *
     * @return unmodifiable map of Category to count
     */
    public Map<Category, Long> getCategoryCounts() {
        return categoryCounts;
    }

    /**
     * Gets average file size in bytes.
     *
     * @return average size in bytes
     */
    public double getAverageSize() {
        return averageSize;
    }

    /**
     * Gets total duplicate file count.
     *
     * @return duplicate count
     */
    public long getDuplicateCount() {
        return duplicateCount;
    }

    /**
     * Gets count of files in a specific Category.
     *
     * @param category category to inspect
     * @return file count
     */
    public long getCategoryCount(Category category) {
        return categoryCounts.getOrDefault(category, 0L);
    }

    /**
     * Returns formatted total file size (e.g., KB, MB, GB).
     *
     * @return formatted size string
     */
    public String getFormattedTotalSize() {
        return formatBytes(totalSize);
    }

    /**
     * Returns formatted average file size.
     *
     * @return formatted average size string
     */
    public String getFormattedAverageSize() {
        return formatBytes((long) averageSize);
    }

    /**
     * Static factory method to compute statistics dynamically from a collection of FileItems.
     *
     * @param files collection of FileItems
     * @return computed Statistics instance
     */
    public static Statistics fromFiles(Collection<FileItem> files) {
        if (files == null || files.isEmpty()) {
            return builder().build();
        }

        long count = files.size();
        long sum = 0;
        long duplicates = 0;
        FileItem largest = null;
        FileItem smallest = null;
        Map<Category, Long> counts = new EnumMap<>(Category.class);

        for (FileItem file : files) {
            if (file == null) continue;

            long size = file.getSize();
            sum += size;

            if (file.isDuplicate()) {
                duplicates++;
            }

            counts.put(file.getCategory(), counts.getOrDefault(file.getCategory(), 0L) + 1);

            if (largest == null || size > largest.getSize()) {
                largest = file;
            }
            if (smallest == null || size < smallest.getSize()) {
                smallest = file;
            }
        }

        double avg = count > 0 ? (double) sum / count : 0.0;

        return builder()
                .totalFiles(count)
                .totalSize(sum)
                .largestFile(largest)
                .smallestFile(smallest)
                .categoryCounts(counts)
                .averageSize(avg)
                .duplicateCount(duplicates)
                .build();
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
        Statistics that = (Statistics) o;
        return totalFiles == that.totalFiles &&
                totalSize == that.totalSize &&
                Double.compare(that.averageSize, averageSize) == 0 &&
                duplicateCount == that.duplicateCount &&
                Objects.equals(largestFile, that.largestFile) &&
                Objects.equals(smallestFile, that.smallestFile) &&
                Objects.equals(categoryCounts, that.categoryCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalFiles, totalSize, largestFile, smallestFile, categoryCounts, averageSize, duplicateCount);
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "totalFiles=" + totalFiles +
                ", totalSize=" + getFormattedTotalSize() +
                ", averageSize=" + getFormattedAverageSize() +
                ", duplicateCount=" + duplicateCount +
                ", categoryCounts=" + categoryCounts +
                '}';
    }

    /**
     * Builder for constructing immutable {@link Statistics} instances.
     */
    public static final class Builder {
        private long totalFiles = 0;
        private long totalSize = 0;
        private FileItem largestFile;
        private FileItem smallestFile;
        private Map<Category, Long> categoryCounts = Collections.emptyMap();
        private double averageSize = 0.0;
        private long duplicateCount = 0;

        public Builder totalFiles(long totalFiles) {
            this.totalFiles = totalFiles;
            return this;
        }

        public Builder totalSize(long totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        public Builder largestFile(FileItem largestFile) {
            this.largestFile = largestFile;
            return this;
        }

        public Builder smallestFile(FileItem smallestFile) {
            this.smallestFile = smallestFile;
            return this;
        }

        public Builder categoryCounts(Map<Category, Long> categoryCounts) {
            this.categoryCounts = categoryCounts != null ? categoryCounts : Collections.emptyMap();
            return this;
        }

        public Builder averageSize(double averageSize) {
            this.averageSize = averageSize;
            return this;
        }

        public Builder duplicateCount(long duplicateCount) {
            this.duplicateCount = duplicateCount;
            return this;
        }

        public Statistics build() {
            return new Statistics(this);
        }
    }
}
