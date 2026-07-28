package com.smartfolderorganizer.service;

/**
 * Event listener interface for receiving file system notifications from {@link FolderWatchService}.
 */
public interface FolderWatchListener {

    /**
     * Invoked when directory watching starts.
     */
    void onStart();

    /**
     * Invoked when a file system change event occurs.
     *
     * @param event detected FolderWatchEvent
     */
    void onEvent(FolderWatchEvent event);

    /**
     * Invoked if an exception occurs during watching.
     *
     * @param ex root cause exception
     */
    void onError(Exception ex);

    /**
     * Invoked when directory watching stops.
     */
    void onStop();
}
