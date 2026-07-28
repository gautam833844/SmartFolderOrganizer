package com.smartfolderorganizer.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Domain model representing a dry-run or preview of an organization task.
 * <p>
 * Aggregates file items, calculates category statistics, and builds the estimated
 * target folder structure before any physical file operations are committed.
 * </p>
 */
public final class OrganizationPreview {

    private final List<FileItem> files;
    private final long totalFiles;
    private final long totalSize;
    private final Map<Category, Long> categoryCounts;
    private final Map<Category, List<FileItem>> estimatedFolderStructure;

    private OrganizationPreview(Builder builder) {
        Objects.requireNonNull(builder.files, "files list must not be null");
        this.files = List.copyOf(builder.files);
        this.totalFiles = this.files.size();
        this.totalSize = this.files.stream().mapToLong(FileItem::getSize).sum();

        Map<Category, Long> countsMap = new EnumMap<>(Category.class);
        Map<Category, List<FileItem>> structureMap = new EnumMap<>(Category.class);

        for (Category cat : Category.values()) {
            countsMap.put(cat, 0L);
            structureMap.put(cat, Collections.emptyList());
        }

        Map<Category, List<FileItem>> grouped = this.files.stream()
                .collect(Collectors.groupingBy(FileItem::getCategory));

        grouped.forEach((cat, itemList) -> {
            countsMap.put(cat, (long) itemList.size());
            structureMap.put(cat, List.copyOf(itemList));
        });

        this.categoryCounts = Collections.unmodifiableMap(countsMap);
        this.estimatedFolderStructure = Collections.unmodifiableMap(structureMap);
    }

    /**
     * Gets all file items in this preview.
     *
     * @return unmodifiable list of FileItems
     */
    public List<FileItem> getFiles() {
        return files;
    }

    /**
     * Gets the total number of files.
     *
     * @return total file count
     */
    public long getTotalFiles() {
        return totalFiles;
    }

    /**
     * Gets the cumulative file size in bytes.
     *
     * @return total bytes
     */
    public long getTotalSize() {
        return totalSize;
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
     * Gets the estimated target folder structure.
     *
     * @return unmodifiable map of Category to list of destination FileItems
     */
    public Map<Category, List<FileItem>> getEstimatedFolderStructure() {
        return estimatedFolderStructure;
    }

    /**
     * Gets total files belonging to a specific category.
     *
     * @param category category to look up
     * @return count of files in category
     */
    public long getCategoryCount(Category category) {
        return categoryCounts.getOrDefault(category, 0L);
    }

    /**
     * Gets files belonging to a specific category.
     *
     * @param category category to look up
     * @return unmodifiable list of FileItems for category
     */
    public List<FileItem> getFilesForCategory(Category category) {
        return estimatedFolderStructure.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Gets only files currently marked as selected.
     *
     * @return list of selected FileItems
     */
    public List<FileItem> getSelectedFiles() {
        return files.stream().filter(FileItem::isSelected).collect(Collectors.toList());
    }

    /**
     * Gets only files marked as duplicate.
     *
     * @return list of duplicate FileItems
     */
    public List<FileItem> getDuplicateFiles() {
        return files.stream().filter(FileItem::isDuplicate).collect(Collectors.toList());
    }

    /**
     * Formats total size into human readable string (e.g., KB, MB, GB).
     *
     * @return formatted size string
     */
    public String getFormattedTotalSize() {
        return formatBytes(totalSize);
    }

    /**
     * Static factory method to construct a preview from a collection of FileItems.
     *
     * @param files list of files to preview
     * @return new OrganizationPreview instance
     */
    public static OrganizationPreview fromFiles(List<FileItem> files) {
        return builder().files(files).build();
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
        OrganizationPreview that = (OrganizationPreview) o;
        return totalFiles == that.totalFiles &&
                totalSize == that.totalSize &&
                Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, totalFiles, totalSize);
    }

    @Override
    public String toString() {
        return "OrganizationPreview{" +
                "totalFiles=" + totalFiles +
                ", totalSize=" + getFormattedTotalSize() +
                ", categoryCounts=" + categoryCounts +
                '}';
    }

    /**
     * Builder for constructing {@link OrganizationPreview} instances.
     */
    public static final class Builder {
        private List<FileItem> files = Collections.emptyList();

        public Builder files(List<FileItem> files) {
            this.files = files != null ? files : Collections.emptyList();
            return this;
        }

        public OrganizationPreview build() {
            return new OrganizationPreview(this);
        }
    }
}
