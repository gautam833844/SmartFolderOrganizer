package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.OrganizationReport;

/**
 * Event listener interface for monitoring file movement operations and lifecycle events.
 */
public interface OrganizationListener {

    /**
     * Invoked when file organization execution starts.
     */
    void onStart();

    /**
     * Invoked periodically with progress snapshots.
     *
     * @param progress current organization progress
     */
    void onProgress(OrganizationProgress progress);

    /**
     * Invoked when an individual file is successfully organized/moved.
     *
     * @param file item moved
     */
    void onFileMoved(FileItem file);

    /**
     * Invoked when a file is skipped due to error, conflict, or selection filter.
     *
     * @param file   item skipped
     * @param reason failure or skip explanation
     */
    void onFileSkipped(FileItem file, String reason);

    /**
     * Invoked when organization completes successfully.
     *
     * @param report complete OrganizationReport summary
     */
    void onComplete(OrganizationReport report);

    /**
     * Invoked if organization was cancelled by user request.
     */
    void onCancelled();

    /**
     * Invoked if a fatal exception halts organization.
     *
     * @param ex root cause exception
     */
    void onError(Exception ex);
}
