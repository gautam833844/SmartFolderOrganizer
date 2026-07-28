package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable event notification representing a file system change detected by {@link FolderWatchService}.
 */
public final class FolderWatchEvent {

    public enum EventType {
        FILE_CREATED,
        FILE_DELETED,
        FILE_MODIFIED,
        DIRECTORY_CREATED,
        DIRECTORY_DELETED
    }

    private final EventType type;
    private final Path path;
    private final LocalDateTime timestamp;
    private final boolean isDirectory;
    private final Category category;

    public FolderWatchEvent(EventType type, Path path, boolean isDirectory, Category category) {
        this(type, path, LocalDateTime.now(), isDirectory, category);
    }

    public FolderWatchEvent(EventType type, Path path, LocalDateTime timestamp, boolean isDirectory, Category category) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.isDirectory = isDirectory;
        this.category = category;
    }

    public EventType getType() {
        return type;
    }

    public Path getPath() {
        return path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public Optional<Category> getCategory() {
        return Optional.ofNullable(category);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FolderWatchEvent event = (FolderWatchEvent) o;
        return isDirectory == event.isDirectory &&
                type == event.type &&
                Objects.equals(path, event.path) &&
                Objects.equals(timestamp, event.timestamp) &&
                category == event.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, path, timestamp, isDirectory, category);
    }

    @Override
    public String toString() {
        return "FolderWatchEvent{" +
                "type=" + type +
                ", path=" + path +
                ", isDirectory=" + isDirectory +
                ", category=" + category +
                ", timestamp=" + timestamp +
                '}';
    }
}
