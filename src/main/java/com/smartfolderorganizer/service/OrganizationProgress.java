package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;

import java.util.Objects;

/**
 * Immutable progress snapshot emitted during file organization operations.
 */
public final class OrganizationProgress {

    private final String currentFile;
    private final long filesProcessed;
    private final long filesRemaining;
    private final double percentage;
    private final Category currentCategory;
    private final String status;

    public OrganizationProgress(String currentFile, long filesProcessed, long filesRemaining, double percentage, Category currentCategory, String status) {
        this.currentFile = currentFile != null ? currentFile : "";
        this.filesProcessed = Math.max(0, filesProcessed);
        this.filesRemaining = Math.max(0, filesRemaining);
        this.percentage = Math.min(100.0, Math.max(0.0, percentage));
        this.currentCategory = currentCategory != null ? currentCategory : Category.OTHERS;
        this.status = status != null ? status : "Processing...";
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public long getFilesProcessed() {
        return filesProcessed;
    }

    public long getFilesRemaining() {
        return filesRemaining;
    }

    public double getPercentage() {
        return percentage;
    }

    public Category getCurrentCategory() {
        return currentCategory;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationProgress progress = (OrganizationProgress) o;
        return filesProcessed == progress.filesProcessed &&
                filesRemaining == progress.filesRemaining &&
                Double.compare(progress.percentage, percentage) == 0 &&
                Objects.equals(currentFile, progress.currentFile) &&
                currentCategory == progress.currentCategory &&
                Objects.equals(status, progress.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentFile, filesProcessed, filesRemaining, percentage, currentCategory, status);
    }

    @Override
    public String toString() {
        return "OrganizationProgress{" +
                "filesProcessed=" + filesProcessed +
                ", filesRemaining=" + filesRemaining +
                ", percentage=" + String.format("%.1f%%", percentage) +
                ", currentCategory=" + currentCategory +
                ", status='" + status + '\'' +
                '}';
    }
}
