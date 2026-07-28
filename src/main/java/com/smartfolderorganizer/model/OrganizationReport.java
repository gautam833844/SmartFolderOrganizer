package com.smartfolderorganizer.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing the final report of an executed organization task.
 * <p>
 * Contains detailed outcomes including successfully organized files, failures, execution duration,
 * audit warnings, statistics summary, and timestamp.
 * </p>
 */
public final class OrganizationReport {

    private final List<FileItem> filesOrganized;
    private final List<FileItem> failedFiles;
    private final List<MoveOperation> moveOperations;
    private final Duration duration;
    private final Statistics statistics;
    private final List<String> warnings;
    private final LocalDateTime executionDate;

    private OrganizationReport(Builder builder) {
        this.filesOrganized = List.copyOf(Objects.requireNonNull(builder.filesOrganized, "filesOrganized must not be null"));
        this.failedFiles = List.copyOf(Objects.requireNonNull(builder.failedFiles, "failedFiles must not be null"));
        this.moveOperations = List.copyOf(Objects.requireNonNull(builder.moveOperations, "moveOperations must not be null"));
        this.duration = Objects.requireNonNull(builder.duration, "duration must not be null");
        this.statistics = Objects.requireNonNull(builder.statistics, "statistics must not be null");
        this.warnings = List.copyOf(Objects.requireNonNull(builder.warnings, "warnings must not be null"));
        this.executionDate = Objects.requireNonNull(builder.executionDate, "executionDate must not be null");
    }

    /**
     * Gets list of successfully organized files.
     *
     * @return unmodifiable list of FileItems
     */
    public List<FileItem> getFilesOrganized() {
        return filesOrganized;
    }

    /**
     * Gets list of files that failed to organize.
     *
     * @return unmodifiable list of FileItems
     */
    public List<FileItem> getFailedFiles() {
        return failedFiles;
    }

    /**
     * Gets detailed audit list of move operations performed.
     *
     * @return unmodifiable list of MoveOperations
     */
    public List<MoveOperation> getMoveOperations() {
        return moveOperations;
    }

    /**
     * Gets total execution duration.
     *
     * @return non-null Duration
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Gets post-organization statistics summary.
     *
     * @return non-null Statistics
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * Gets any non-fatal warnings emitted during organization.
     *
     * @return unmodifiable list of warning strings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Gets date and time when organization was executed.
     *
     * @return non-null LocalDateTime
     */
    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    /**
     * Gets total count of successfully organized files.
     *
     * @return count
     */
    public int getOrganizedCount() {
        return filesOrganized.size();
    }

    /**
     * Gets total count of failed files.
     *
     * @return count
     */
    public int getFailedCount() {
        return failedFiles.size();
    }

    /**
     * Checks whether organization completed without any file failures.
     *
     * @return true if 0 failures
     */
    public boolean isSuccessful() {
        return failedFiles.isEmpty();
    }

    /**
     * Returns formatted execution duration (e.g. "1.23s" or "250ms").
     *
     * @return formatted duration string
     */
    public String getFormattedDuration() {
        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + " ms";
        }
        return String.format("%.2f s", millis / 1000.0);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationReport report = (OrganizationReport) o;
        return Objects.equals(executionDate, report.executionDate) &&
                Objects.equals(filesOrganized, report.filesOrganized) &&
                Objects.equals(failedFiles, report.failedFiles) &&
                Objects.equals(duration, report.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionDate, filesOrganized, failedFiles, duration);
    }

    @Override
    public String toString() {
        return "OrganizationReport{" +
                "executionDate=" + executionDate +
                ", organizedCount=" + getOrganizedCount() +
                ", failedCount=" + getFailedCount() +
                ", duration=" + getFormattedDuration() +
                ", warningsCount=" + warnings.size() +
                '}';
    }

    /**
     * Builder for constructing immutable {@link OrganizationReport} instances.
     */
    public static final class Builder {
        private List<FileItem> filesOrganized = Collections.emptyList();
        private List<FileItem> failedFiles = Collections.emptyList();
        private List<MoveOperation> moveOperations = Collections.emptyList();
        private Duration duration = Duration.ZERO;
        private Statistics statistics;
        private List<String> warnings = Collections.emptyList();
        private LocalDateTime executionDate;

        public Builder() {
            this.executionDate = LocalDateTime.now();
        }

        public Builder filesOrganized(List<FileItem> filesOrganized) {
            this.filesOrganized = filesOrganized != null ? filesOrganized : Collections.emptyList();
            return this;
        }

        public Builder failedFiles(List<FileItem> failedFiles) {
            this.failedFiles = failedFiles != null ? failedFiles : Collections.emptyList();
            return this;
        }

        public Builder moveOperations(List<MoveOperation> moveOperations) {
            this.moveOperations = moveOperations != null ? moveOperations : Collections.emptyList();
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration != null ? duration : Duration.ZERO;
            return this;
        }

        public Builder statistics(Statistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings != null ? warnings : Collections.emptyList();
            return this;
        }

        public Builder executionDate(LocalDateTime executionDate) {
            this.executionDate = executionDate;
            return this;
        }

        public OrganizationReport build() {
            if (this.executionDate == null) {
                this.executionDate = LocalDateTime.now();
            }
            if (this.duration == null) {
                this.duration = Duration.ZERO;
            }
            if (this.statistics == null) {
                this.statistics = Statistics.fromFiles(this.filesOrganized);
            }
            return new OrganizationReport(this);
        }
    }
}
