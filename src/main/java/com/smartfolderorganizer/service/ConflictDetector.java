package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.util.FileUtils;
import com.smartfolderorganizer.util.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utility service for detecting path collisions, missing write permissions, unmapped categories, and destination conflicts.
 */
public final class ConflictDetector {

    private ConflictDetector() {
        throw new UnsupportedOperationException("Utility class 'ConflictDetector' cannot be instantiated");
    }

    /**
     * Inspects a list of preview FileItems against a root destination directory to identify conflicts and warnings.
     *
     * @param items           list of FileItems with computed destination paths
     * @param rootDestination root organization target path (nullable/optional)
     * @return list of formatted conflict warning messages
     */
    public static List<String> detectConflicts(List<FileItem> items, Path rootDestination) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> conflicts = new ArrayList<>();
        Set<Path> uniqueDestinations = new HashSet<>();

        if (rootDestination != null) {
            if (!Files.exists(rootDestination)) {
                conflicts.add("Root destination folder does not exist on disk: " + rootDestination);
            } else if (!Files.isDirectory(rootDestination)) {
                conflicts.add("Root destination is not a directory: " + rootDestination);
            } else if (!PathUtils.isWritable(rootDestination)) {
                conflicts.add("Root destination directory is not writable: " + rootDestination);
            }
        }

        for (FileItem item : items) {
            if (item == null) continue;

            Path dest = item.getDestinationPath();
            if (dest == null) {
                conflicts.add("Missing destination path for file: " + item.getFileName());
                continue;
            }

            // Check duplicate target paths within preview set
            if (!uniqueDestinations.add(dest)) {
                conflicts.add("Destination path collision detected between multiple files: " + dest);
            }

            // Check if file already exists at target destination on disk
            if (FileUtils.exists(dest) && !dest.equals(item.getOriginalPath())) {
                conflicts.add(String.format("File '%s' already exists at destination: %s", item.getFileName(), dest));
            }

            // Check for unmapped Category.OTHERS
            if (item.getCategory() == Category.OTHERS) {
                conflicts.add("File has uncategorized category 'OTHERS': " + item.getFileName());
            }

            // Prevent source and destination nesting or identity loops
            if (item.getOriginalPath().equals(dest)) {
                conflicts.add("Source and destination paths are identical for file: " + item.getFileName());
            }
        }

        return Collections.unmodifiableList(conflicts);
    }
}
