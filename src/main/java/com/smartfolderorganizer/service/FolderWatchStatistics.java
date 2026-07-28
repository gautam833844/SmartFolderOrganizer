package com.smartfolderorganizer.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics tracking runtime events and counters for {@link FolderWatchService}.
 */
public class FolderWatchStatistics {

    private final AtomicLong eventsReceived = new AtomicLong(0);
    private final AtomicLong filesCreated = new AtomicLong(0);
    private final AtomicLong filesDeleted = new AtomicLong(0);
    private final AtomicLong filesModified = new AtomicLong(0);
    private final AtomicLong directoriesCreated = new AtomicLong(0);
    private final AtomicLong directoriesDeleted = new AtomicLong(0);
    private volatile Instant startTime = Instant.now();

    public void incrementEventsReceived() {
        eventsReceived.incrementAndGet();
    }

    public void incrementFilesCreated() {
        filesCreated.incrementAndGet();
        incrementEventsReceived();
    }

    public void incrementFilesDeleted() {
        filesDeleted.incrementAndGet();
        incrementEventsReceived();
    }

    public void incrementFilesModified() {
        filesModified.incrementAndGet();
        incrementEventsReceived();
    }

    public void incrementDirectoriesCreated() {
        directoriesCreated.incrementAndGet();
        incrementEventsReceived();
    }

    public void incrementDirectoriesDeleted() {
        directoriesDeleted.incrementAndGet();
        incrementEventsReceived();
    }

    public long getEventsReceived() {
        return eventsReceived.get();
    }

    public long getFilesCreated() {
        return filesCreated.get();
    }

    public long getFilesDeleted() {
        return filesDeleted.get();
    }

    public long getFilesModified() {
        return filesModified.get();
    }

    public long getDirectoriesCreated() {
        return directoriesCreated.get();
    }

    public long getDirectoriesDeleted() {
        return directoriesDeleted.get();
    }

    public Duration getRuntime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Resets all event counters and restarts the runtime timer.
     */
    public void reset() {
        eventsReceived.set(0);
        filesCreated.set(0);
        filesDeleted.set(0);
        filesModified.set(0);
        directoriesCreated.set(0);
        directoriesDeleted.set(0);
        startTime = Instant.now();
    }

    @Override
    public String toString() {
        return "FolderWatchStatistics{" +
                "eventsReceived=" + getEventsReceived() +
                ", filesCreated=" + getFilesCreated() +
                ", filesDeleted=" + getFilesDeleted() +
                ", filesModified=" + getFilesModified() +
                ", directoriesCreated=" + getDirectoriesCreated() +
                ", directoriesDeleted=" + getDirectoriesDeleted() +
                ", runtime=" + getRuntime().toSeconds() + "s" +
                '}';
    }
}
