package com.smartfolderorganizer.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome summary of an undo operation.
 */
public final class UndoResult {

    private final boolean successful;
    private final int restoredFiles;
    private final int failedFiles;
    private final List<String> errors;
    private final Duration duration;

    private UndoResult(Builder builder) {
        this.restoredFiles = builder.restoredFiles;
        this.failedFiles = builder.failedFiles;
        this.errors = List.copyOf(Objects.requireNonNull(builder.errors, "errors must not be null"));
        this.duration = Objects.requireNonNull(builder.duration, "duration must not be null");
        this.successful = builder.successful && errors.isEmpty() && failedFiles == 0;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public int getRestoredFiles() {
        return restoredFiles;
    }

    public int getFailedFiles() {
        return failedFiles;
    }

    public List<String> getErrors() {
        return errors;
    }

    public Duration getDuration() {
        return duration;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoResult that = (UndoResult) o;
        return successful == that.successful &&
                restoredFiles == that.restoredFiles &&
                failedFiles == that.failedFiles &&
                Objects.equals(errors, that.errors) &&
                Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(successful, restoredFiles, failedFiles, errors, duration);
    }

    @Override
    public String toString() {
        return "UndoResult{" +
                "successful=" + successful +
                ", restoredFiles=" + restoredFiles +
                ", failedFiles=" + failedFiles +
                ", errorsCount=" + errors.size() +
                ", duration=" + duration.toMillis() + "ms" +
                '}';
    }

    /**
     * Builder for constructing immutable {@link UndoResult}.
     */
    public static final class Builder {
        private boolean successful = true;
        private int restoredFiles = 0;
        private int failedFiles = 0;
        private List<String> errors = Collections.emptyList();
        private Duration duration = Duration.ZERO;

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder restoredFiles(int restoredFiles) {
            this.restoredFiles = restoredFiles;
            return this;
        }

        public Builder failedFiles(int failedFiles) {
            this.failedFiles = failedFiles;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors != null ? errors : Collections.emptyList();
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration != null ? duration : Duration.ZERO;
            return this;
        }

        public UndoResult build() {
            return new UndoResult(this);
        }
    }
}
