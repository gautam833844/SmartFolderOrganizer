package com.smartfolderorganizer.service;

/**
 * Event listener interface for receiving lifecycle and progress updates from {@link ScanService}.
 */
public interface ScanListener {

    /**
     * Invoked when directory scanning begins.
     */
    void onStart();

    /**
     * Invoked periodically with updated scan statistics and progress snapshots.
     *
     * @param progress current scan progress snapshot
     */
    void onProgress(ScanProgress progress);

    /**
     * Invoked when directory scanning completes successfully.
     *
     * @param result aggregated scan results
     */
    void onComplete(ScanResult result);

    /**
     * Invoked if scanning was cancelled by the user.
     */
    void onCancelled();

    /**
     * Invoked if an unrecoverable error occurs during scanning initialization or execution.
     *
     * @param error root cause exception
     */
    void onError(Throwable error);
}
