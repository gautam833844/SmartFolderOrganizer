package com.smartfolderorganizer.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Thread-safe service maintaining, deduplicating, and persisting the list of recently accessed folder paths.
 */
public class RecentFoldersService {

    public static final int DEFAULT_MAX_RECENT = 10;

    private final Path filePath;
    private final int maxEntries;
    private final ObjectMapper mapper;
    private final List<String> recentFolders = new ArrayList<>();

    public RecentFoldersService() {
        this(PersistenceConstants.getDefaultRecentFoldersFilePath(), DEFAULT_MAX_RECENT);
    }

    public RecentFoldersService(Path filePath, int maxEntries) {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.maxEntries = Math.max(1, maxEntries);
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        load();
    }

    /**
     * Adds a folder path to the top of recent folders history, removing duplicates.
     *
     * @param folderPath path string to add
     */
    public synchronized void addFolder(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) return;

        String clean = normalizePath(folderPath);

        // Remove existing duplicate
        recentFolders.removeIf(p -> normalizePath(p).equals(clean));

        // Insert at head
        recentFolders.add(0, clean);

        // Trim capacity
        while (recentFolders.size() > maxEntries) {
            recentFolders.remove(recentFolders.size() - 1);
        }

        save();
    }

    /**
     * Removes a folder from recent history.
     *
     * @param folderPath path to remove
     */
    public synchronized void removeFolder(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) return;
        String clean = normalizePath(folderPath);
        if (recentFolders.removeIf(p -> normalizePath(p).equals(clean))) {
            save();
        }
    }

    /**
     * Returns an unmodifiable list of recent folder path strings.
     *
     * @return unmodifiable list
     */
    public synchronized List<String> getRecentFolders() {
        return Collections.unmodifiableList(new ArrayList<>(recentFolders));
    }

    /**
     * Clears all recent folders and persists change.
     */
    public synchronized void clear() {
        recentFolders.clear();
        save();
    }

    public synchronized void load() {
        recentFolders.clear();
        if (!Files.exists(filePath)) return;

        try {
            List<String> loaded = mapper.readValue(filePath.toFile(), new TypeReference<List<String>>() {});
            if (loaded != null) {
                for (String path : loaded) {
                    if (path != null && !path.isBlank()) {
                        String clean = normalizePath(path);
                        if (!recentFolders.contains(clean)) {
                            recentFolders.add(clean);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // Graceful fallback on corrupt file
        }
    }

    public synchronized boolean save() {
        try {
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            mapper.writeValue(filePath.toFile(), recentFolders);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        return path.trim().replace('\\', '/').replaceAll("/+$", "");
    }
}
