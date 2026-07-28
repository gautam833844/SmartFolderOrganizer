package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.OrganizationPreview;
import com.smartfolderorganizer.model.Statistics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable aggregated result containing dry-run organization preview, folder structure maps, conflict analyses, and statistics.
 */
public final class PreviewResult {

    private final OrganizationPreview preview;
    private final Map<Category, List<FileItem>> folderStructure;
    private final Statistics statistics;
    private final PreviewStatistics previewStatistics;
    private final boolean successful;
    private final List<String> warnings;
    private final List<String> conflicts;

    private PreviewResult(Builder builder) {
        this.preview = Objects.requireNonNull(builder.preview, "preview must not be null");
        this.folderStructure = Collections.unmodifiableMap(
                builder.folderStructure != null ? builder.folderStructure : preview.getEstimatedFolderStructure()
        );
        this.statistics = builder.statistics != null ? builder.statistics : Statistics.fromFiles(preview.getFiles());
        this.previewStatistics = builder.previewStatistics != null
                ? builder.previewStatistics
                : PreviewStatistics.calculate(preview.getFiles());
        this.conflicts = List.copyOf(Objects.requireNonNull(builder.conflicts, "conflicts must not be null"));
        this.warnings = List.copyOf(Objects.requireNonNull(builder.warnings, "warnings must not be null"));
        this.successful = builder.successful && conflicts.isEmpty();
    }

    public OrganizationPreview getPreview() {
        return preview;
    }

    public Map<Category, List<FileItem>> getFolderStructure() {
        return folderStructure;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public PreviewStatistics getPreviewStatistics() {
        return previewStatistics;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreviewResult that = (PreviewResult) o;
        return successful == that.successful &&
                Objects.equals(preview, that.preview) &&
                Objects.equals(conflicts, that.conflicts) &&
                Objects.equals(warnings, that.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preview, successful, conflicts, warnings);
    }

    @Override
    public String toString() {
        return "PreviewResult{" +
                "successful=" + successful +
                ", filesCount=" + preview.getTotalFiles() +
                ", conflictsCount=" + conflicts.size() +
                ", warningsCount=" + warnings.size() +
                '}';
    }

    /**
     * Builder for constructing immutable {@link PreviewResult}.
     */
    public static final class Builder {
        private OrganizationPreview preview;
        private Map<Category, List<FileItem>> folderStructure;
        private Statistics statistics;
        private PreviewStatistics previewStatistics;
        private boolean successful = true;
        private List<String> warnings = Collections.emptyList();
        private List<String> conflicts = Collections.emptyList();

        public Builder preview(OrganizationPreview preview) {
            this.preview = preview;
            return this;
        }

        public Builder folderStructure(Map<Category, List<FileItem>> folderStructure) {
            this.folderStructure = folderStructure;
            return this;
        }

        public Builder statistics(Statistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Builder previewStatistics(PreviewStatistics previewStatistics) {
            this.previewStatistics = previewStatistics;
            return this;
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings != null ? warnings : Collections.emptyList();
            return this;
        }

        public Builder conflicts(List<String> conflicts) {
            this.conflicts = conflicts != null ? conflicts : Collections.emptyList();
            return this;
        }

        public PreviewResult build() {
            return new PreviewResult(this);
        }
    }
}
