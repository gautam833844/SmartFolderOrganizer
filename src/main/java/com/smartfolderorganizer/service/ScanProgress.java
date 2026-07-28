package com.smartfolderorganizer.service;

import java.util.Objects;

/**
 * Immutable progress snapshot emitted during an active directory scan.
 */
public final class ScanProgress {

    private final String currentFile;
    private final long filesScanned;
    private final long directoriesScanned;
    private final double percentage;
    private final String status;

    public ScanProgress(String currentFile, long filesScanned, long directoriesScanned, double percentage, String status) {
        this.currentFile = currentFile != null ? currentFile : "";
        this.filesScanned = Math.max(0, filesScanned);
        this.directoriesScanned = Math.max(0, directoriesScanned);
        this.percentage = Math.min(100.0, Math.max(0.0, percentage));
        this.status = status != null ? status : "Scanning...";
    }

    /**
     * Gets the file or directory currently being processed.
     *
     * @return current file path string
     */
    public String getCurrentFile() {
        return currentFile;
    }

    /**
     * Gets total count of files scanned so far.
     *
     * @return files count
     */
    public long getFilesScanned() {
        return filesScanned;
    }

    /**
     * Gets total count of directories scanned so far.
     *
     * @return directories count
     */
    public long getDirectoriesScanned() {
        return directoriesScanned;
    }

    /**
     * Gets estimated progress percentage (0.0 to 100.0).
     *
     * @return progress percentage
     */
    public double getPercentage() {
        return percentage;
    }

    /**
     * Gets current status message.
     *
     * @return status description string
     */
    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScanProgress progress = (ScanProgress) o;
        return filesScanned == progress.filesScanned &&
                directoriesScanned == progress.directoriesScanned &&
                Double.compare(progress.percentage, percentage) == 0 &&
                Objects.equals(currentFile, progress.currentFile) &&
                Objects.equals(status, progress.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentFile, filesScanned, directoriesScanned, percentage, status);
    }

    @Override
    public String toString() {
        return "ScanProgress{" +
                "filesScanned=" + filesScanned +
                ", directoriesScanned=" + directoriesScanned +
                ", percentage=" + String.format("%.1f%%", percentage) +
                ", status='" + status + '\'' +
                ", currentFile='" + currentFile + '\'' +
                '}';
    }
}
