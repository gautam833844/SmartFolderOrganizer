package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.DuplicateGroup;

/**
 * Event listener interface for monitoring duplicate detection operations.
 */
public interface DuplicateDetectionListener {

    /**
     * Invoked when duplicate detection starts.
     */
    void onStart();

    /**
     * Invoked periodically with progress counters.
     *
     * @param scanned count of files scanned/inspected so far
     * @param total   total candidate files
     */
    void onProgress(long scanned, long total);

    /**
     * Invoked when a new duplicate group is discovered.
     *
     * @param group discovered DuplicateGroup
     */
    void onDuplicateFound(DuplicateGroup group);

    /**
     * Invoked when duplicate detection completes.
     *
     * @param result aggregated DuplicateDetectionResult
     */
    void onComplete(DuplicateDetectionResult result);

    /**
     * Invoked if duplicate detection was cancelled by user.
     */
    void onCancelled();

    /**
     * Invoked if an exception occurs during detection.
     *
     * @param ex root cause exception
     */
    void onError(Exception ex);
}
