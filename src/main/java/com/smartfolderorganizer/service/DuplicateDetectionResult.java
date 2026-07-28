package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.DuplicateGroup;
import com.smartfolderorganizer.util.SizeFormatter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome summary of a duplicate file detection scan.
 */
public final class DuplicateDetectionResult {

    private final List<DuplicateGroup> duplicateGroups;
    private final long filesScanned;
    private final long duplicateFiles;
    private final long duplicateGroupsCount;
    private final long totalDuplicateSize;
    private final Duration duration;
    private final List<String> errors;

    private DuplicateDetectionResult(Builder builder) {
        this.duplicateGroups = List.copyOf(Objects.requireNonNull(builder.duplicateGroups, "duplicateGroups must not be null"));
        this.filesScanned = builder.filesScanned;
        this.duplicateFiles = builder.duplicateFiles >= 0
                ? builder.duplicateFiles
                : this.duplicateGroups.stream().mapToLong(g -> g.getFiles().size() - 1).sum();
        this.duplicateGroupsCount = this.duplicateGroups.size();
        this.totalDuplicateSize = builder.totalDuplicateSize >= 0
                ? builder.totalDuplicateSize
                : this.duplicateGroups.stream().mapToLong(DuplicateGroup::getDuplicateSize).sum();
        this.duration = Objects.requireNonNull(builder.duration, "duration must not be null");
        this.errors = List.copyOf(Objects.requireNonNull(builder.errors, "errors must not be null"));
    }

    public List<DuplicateGroup> getDuplicateGroups() {
        return duplicateGroups;
    }

    public long getFilesScanned() {
        return filesScanned;
    }

    public long getDuplicateFiles() {
        return duplicateFiles;
    }

    public long getDuplicateGroupsCount() {
        return duplicateGroupsCount;
    }

    public long getTotalDuplicateSize() {
        return totalDuplicateSize;
    }

    public String getFormattedTotalDuplicateSize() {
        return SizeFormatter.formatDecimal(totalDuplicateSize);
    }

    public Duration getDuration() {
        return duration;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuplicateDetectionResult result = (DuplicateDetectionResult) o;
        return filesScanned == result.filesScanned &&
                duplicateFiles == result.duplicateFiles &&
                duplicateGroupsCount == result.duplicateGroupsCount &&
                totalDuplicateSize == result.totalDuplicateSize &&
                Objects.equals(duplicateGroups, result.duplicateGroups) &&
                Objects.equals(duration, result.duration) &&
                Objects.equals(errors, result.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duplicateGroups, filesScanned, duplicateFiles, duplicateGroupsCount, totalDuplicateSize, duration, errors);
    }

    @Override
    public String toString() {
        return "DuplicateDetectionResult{" +
                "duplicateGroupsCount=" + duplicateGroupsCount +
                ", duplicateFiles=" + duplicateFiles +
                ", totalDuplicateSize=" + getFormattedTotalDuplicateSize() +
                ", filesScanned=" + filesScanned +
                ", duration=" + duration.toMillis() + "ms" +
                '}';
    }

    /**
     * Builder for constructing immutable {@link DuplicateDetectionResult}.
     */
    public static final class Builder {
        private List<DuplicateGroup> duplicateGroups = Collections.emptyList();
        private long filesScanned = 0;
        private long duplicateFiles = -1;
        private long totalDuplicateSize = -1;
        private Duration duration = Duration.ZERO;
        private List<String> errors = Collections.emptyList();

        public Builder duplicateGroups(List<DuplicateGroup> duplicateGroups) {
            this.duplicateGroups = duplicateGroups != null ? duplicateGroups : Collections.emptyList();
            return this;
        }

        public Builder filesScanned(long filesScanned) {
            this.filesScanned = filesScanned;
            return this;
        }

        public Builder duplicateFiles(long duplicateFiles) {
            this.duplicateFiles = duplicateFiles;
            return this;
        }

        public Builder totalDuplicateSize(long totalDuplicateSize) {
            this.totalDuplicateSize = totalDuplicateSize;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration != null ? duration : Duration.ZERO;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors != null ? errors : Collections.emptyList();
            return this;
        }

        public DuplicateDetectionResult build() {
            return new DuplicateDetectionResult(this);
        }
    }
}
