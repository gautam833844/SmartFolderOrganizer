package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.Statistics;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable aggregated outcome of a completed directory scan operation.
 */
public final class ScanResult {

    private final List<FileItem> files;
    private final Statistics statistics;
    private final Duration duration;
    private final long filesScanned;
    private final long directoriesScanned;
    private final long skippedFiles;
    private final List<String> errors;

    private ScanResult(Builder builder) {
        this.files = List.copyOf(Objects.requireNonNull(builder.files, "files must not be null"));
        this.statistics = builder.statistics != null ? builder.statistics : Statistics.fromFiles(this.files);
        this.duration = Objects.requireNonNull(builder.duration, "duration must not be null");
        this.filesScanned = builder.filesScanned;
        this.directoriesScanned = builder.directoriesScanned;
        this.skippedFiles = builder.skippedFiles;
        this.errors = List.copyOf(Objects.requireNonNull(builder.errors, "errors must not be null"));
    }

    public List<FileItem> getFiles() {
        return files;
    }

    public List<FileItem> getScannedFiles() {
        return files;
    }

    public long getTotalFiles() {
        return filesScanned;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public Duration getDuration() {
        return duration;
    }

    public long getFilesScanned() {
        return filesScanned;
    }

    public long getDirectoriesScanned() {
        return directoriesScanned;
    }

    public long getSkippedFiles() {
        return skippedFiles;
    }

    public List<String> getErrors() {
        return errors;
    }

    /**
     * Checks if the scan completed without incurring any file access or I/O errors.
     *
     * @return true if error list is empty
     */
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
        ScanResult that = (ScanResult) o;
        return filesScanned == that.filesScanned &&
                directoriesScanned == that.directoriesScanned &&
                skippedFiles == that.skippedFiles &&
                Objects.equals(files, that.files) &&
                Objects.equals(duration, that.duration) &&
                Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, duration, filesScanned, directoriesScanned, skippedFiles, errors);
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "filesCollected=" + files.size() +
                ", filesScanned=" + filesScanned +
                ", directoriesScanned=" + directoriesScanned +
                ", skippedFiles=" + skippedFiles +
                ", errorsCount=" + errors.size() +
                ", duration=" + duration.toMillis() + "ms" +
                '}';
    }

    /**
     * Builder for constructing immutable {@link ScanResult}.
     */
    public static final class Builder {
        private List<FileItem> files = Collections.emptyList();
        private Statistics statistics;
        private Duration duration = Duration.ZERO;
        private long filesScanned = 0;
        private long directoriesScanned = 0;
        private long skippedFiles = 0;
        private List<String> errors = Collections.emptyList();

        public Builder files(List<FileItem> files) {
            this.files = files != null ? files : Collections.emptyList();
            return this;
        }

        public Builder statistics(Statistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration != null ? duration : Duration.ZERO;
            return this;
        }

        public Builder filesScanned(long filesScanned) {
            this.filesScanned = filesScanned;
            return this;
        }

        public Builder directoriesScanned(long directoriesScanned) {
            this.directoriesScanned = directoriesScanned;
            return this;
        }

        public Builder skippedFiles(long skippedFiles) {
            this.skippedFiles = skippedFiles;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors != null ? errors : Collections.emptyList();
            return this;
        }

        public ScanResult build() {
            return new ScanResult(this);
        }
    }
}
