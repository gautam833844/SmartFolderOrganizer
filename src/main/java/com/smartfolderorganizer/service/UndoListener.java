package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.FileItem;

/**
 * Event listener interface for monitoring transaction undo operations.
 */
public interface UndoListener {

    /**
     * Invoked when an undo execution starts.
     */
    void onStart();

    /**
     * Invoked periodically with restoration progress stats.
     *
     * @param restored count of files restored so far
     * @param total    total files scheduled for restoration
     */
    void onProgress(int restored, int total);

    /**
     * Invoked when a file is successfully restored to its original source location.
     *
     * @param file restored file item
     */
    void onFileRestored(FileItem file);

    /**
     * Invoked when a file restoration is skipped or fails.
     *
     * @param file   file item skipped
     * @param reason failure or skip reason
     */
    void onFileSkipped(FileItem file, String reason);

    /**
     * Invoked when undo execution finishes.
     *
     * @param result aggregated UndoResult summary
     */
    void onComplete(UndoResult result);

    /**
     * Invoked if undo was cancelled by user request.
     */
    void onCancelled();

    /**
     * Invoked if an unrecoverable error occurs during undo.
     *
     * @param ex root cause exception
     */
    void onError(Exception ex);
}
