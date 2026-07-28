package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.util.SizeFormatter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable statistical metrics specifically computed for preview organization scenarios.
 */
public final class PreviewStatistics {

    private final Map<Category, Long> filesPerCategory;
    private final Map<Category, Long> totalSizePerCategory;
    private final Category largestCategory;
    private final Category smallestCategory;
    private final int estimatedFolderCount;
    private final long estimatedSpaceSaved;

    private PreviewStatistics(Builder builder) {
        this.filesPerCategory = Collections.unmodifiableMap(builder.filesPerCategory);
        this.totalSizePerCategory = Collections.unmodifiableMap(builder.totalSizePerCategory);
        this.largestCategory = builder.largestCategory != null ? builder.largestCategory : Category.OTHERS;
        this.smallestCategory = builder.smallestCategory != null ? builder.smallestCategory : Category.OTHERS;
        this.estimatedFolderCount = builder.estimatedFolderCount;
        this.estimatedSpaceSaved = builder.estimatedSpaceSaved;
    }

    public Map<Category, Long> getFilesPerCategory() {
        return filesPerCategory;
    }

    public Map<Category, Long> getTotalSizePerCategory() {
        return totalSizePerCategory;
    }

    public Category getLargestCategory() {
        return largestCategory;
    }

    public Category getSmallestCategory() {
        return smallestCategory;
    }

    public int getEstimatedFolderCount() {
        return estimatedFolderCount;
    }

    public long getEstimatedSpaceSaved() {
        return estimatedSpaceSaved;
    }

    public String getFormattedSpaceSaved() {
        return SizeFormatter.formatDecimal(estimatedSpaceSaved);
    }

    /**
     * Calculates PreviewStatistics from a list of preview FileItems.
     *
     * @param files list of FileItems
     * @return calculated PreviewStatistics instance
     */
    public static PreviewStatistics calculate(List<FileItem> files) {
        if (files == null || files.isEmpty()) {
            return builder().build();
        }

        Map<Category, Long> countMap = new EnumMap<>(Category.class);
        Map<Category, Long> sizeMap = new EnumMap<>(Category.class);

        for (Category cat : Category.values()) {
            countMap.put(cat, 0L);
            sizeMap.put(cat, 0L);
        }

        long spaceSaved = 0L;

        for (FileItem file : files) {
            if (file == null) continue;
            Category cat = file.getCategory() != null ? file.getCategory() : Category.OTHERS;
            countMap.put(cat, countMap.getOrDefault(cat, 0L) + 1);
            sizeMap.put(cat, sizeMap.getOrDefault(cat, 0L) + file.getSize());

            if (file.isDuplicate()) {
                spaceSaved += file.getSize();
            }
        }

        Category largest = Category.OTHERS;
        Category smallest = null;
        long maxSize = -1L;
        long minSize = Long.MAX_VALUE;
        int activeFolderCount = 0;

        for (Map.Entry<Category, Long> entry : sizeMap.entrySet()) {
            long count = countMap.getOrDefault(entry.getKey(), 0L);
            if (count > 0) {
                activeFolderCount++;
                long size = entry.getValue();
                if (size > maxSize) {
                    maxSize = size;
                    largest = entry.getKey();
                }
                if (size < minSize) {
                    minSize = size;
                    smallest = entry.getKey();
                }
            }
        }

        if (smallest == null) {
            smallest = Category.OTHERS;
        }

        return builder()
                .filesPerCategory(countMap)
                .totalSizePerCategory(sizeMap)
                .largestCategory(largest)
                .smallestCategory(smallest)
                .estimatedFolderCount(activeFolderCount)
                .estimatedSpaceSaved(spaceSaved)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreviewStatistics that = (PreviewStatistics) o;
        return estimatedFolderCount == that.estimatedFolderCount &&
                estimatedSpaceSaved == that.estimatedSpaceSaved &&
                Objects.equals(filesPerCategory, that.filesPerCategory) &&
                Objects.equals(totalSizePerCategory, that.totalSizePerCategory) &&
                largestCategory == that.largestCategory &&
                smallestCategory == that.smallestCategory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filesPerCategory, totalSizePerCategory, largestCategory, smallestCategory, estimatedFolderCount, estimatedSpaceSaved);
    }

    @Override
    public String toString() {
        return "PreviewStatistics{" +
                "estimatedFolderCount=" + estimatedFolderCount +
                ", largestCategory=" + largestCategory +
                ", estimatedSpaceSaved=" + getFormattedSpaceSaved() +
                '}';
    }

    /**
     * Builder for constructing immutable {@link PreviewStatistics}.
     */
    public static final class Builder {
        private Map<Category, Long> filesPerCategory = Collections.emptyMap();
        private Map<Category, Long> totalSizePerCategory = Collections.emptyMap();
        private Category largestCategory = Category.OTHERS;
        private Category smallestCategory = Category.OTHERS;
        private int estimatedFolderCount = 0;
        private long estimatedSpaceSaved = 0L;

        public Builder filesPerCategory(Map<Category, Long> filesPerCategory) {
            this.filesPerCategory = filesPerCategory != null ? filesPerCategory : Collections.emptyMap();
            return this;
        }

        public Builder totalSizePerCategory(Map<Category, Long> totalSizePerCategory) {
            this.totalSizePerCategory = totalSizePerCategory != null ? totalSizePerCategory : Collections.emptyMap();
            return this;
        }

        public Builder largestCategory(Category largestCategory) {
            this.largestCategory = largestCategory;
            return this;
        }

        public Builder smallestCategory(Category smallestCategory) {
            this.smallestCategory = smallestCategory;
            return this;
        }

        public Builder estimatedFolderCount(int estimatedFolderCount) {
            this.estimatedFolderCount = estimatedFolderCount;
            return this;
        }

        public Builder estimatedSpaceSaved(long estimatedSpaceSaved) {
            this.estimatedSpaceSaved = estimatedSpaceSaved;
            return this;
        }

        public PreviewStatistics build() {
            return new PreviewStatistics(this);
        }
    }
}
