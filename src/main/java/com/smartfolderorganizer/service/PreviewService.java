package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.OrganizationPreview;
import com.smartfolderorganizer.util.FileUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for computing dry-run organization previews, destination path assignments, conflict detections, and preview statistics.
 */
public class PreviewService {

    private final CategoryService categoryService;
    private final AtomicReference<PreviewResult> cachedPreview = new AtomicReference<>(null);

    public PreviewService() {
        this(new CategoryService());
    }

    public PreviewService(CategoryService categoryService) {
        this.categoryService = Objects.requireNonNull(categoryService, "CategoryService must not be null");
    }

    /**
     * Generates an OrganizationPreview assuming target destination folders are relative to each item's parent directory.
     *
     * @param files input list of FileItems
     * @return generated PreviewResult
     */
    public PreviewResult generatePreview(List<FileItem> files) {
        return generatePreview(files, null);
    }

    /**
     * Generates a complete organization preview assigning target destination paths relative to a root destination folder.
     *
     * @param files           input list of FileItems (non-null)
     * @param rootDestination target root directory for organization (nullable)
     * @return computed PreviewResult containing previews, statistics, and conflict analysis
     */
    public PreviewResult generatePreview(List<FileItem> files, Path rootDestination) {
        if (files == null || files.isEmpty()) {
            PreviewResult emptyResult = createEmptyPreviewResult();
            cachedPreview.set(emptyResult);
            return emptyResult;
        }

        List<FileItem> processedItems = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<Path, Set<String>> pathFileNameTracker = new HashMap<>();

        for (FileItem item : files) {
            if (item == null) continue;

            Category category = item.getCategory();
            if (category == null || category == Category.OTHERS) {
                category = categoryService.detectCategory(item.getOriginalPath());
            }

            Path parentDir = rootDestination != null
                    ? rootDestination.resolve(category.getFolderName())
                    : item.getOriginalPath().getParent().resolve(category.getFolderName());

            String baseName = item.getFileName();
            Path proposedDest = parentDir.resolve(baseName);

            // Path collision resolution in preview
            Set<String> existingNames = pathFileNameTracker.computeIfAbsent(parentDir, k -> new HashSet<>());
            if (existingNames.contains(baseName.toLowerCase())) {
                warnings.add("Duplicate filename detected in preview destination: " + proposedDest);
                proposedDest = FileUtils.generateUniquePath(proposedDest);
            }
            existingNames.add(proposedDest.getFileName().toString().toLowerCase());

            FileItem updatedItem = item.toBuilder()
                    .category(category)
                    .destinationPath(proposedDest)
                    .build();

            processedItems.add(updatedItem);
        }

        OrganizationPreview preview = OrganizationPreview.fromFiles(processedItems);
        List<String> conflicts = ConflictDetector.detectConflicts(processedItems, rootDestination);

        PreviewResult result = PreviewResult.builder()
                .preview(preview)
                .warnings(warnings)
                .conflicts(conflicts)
                .successful(conflicts.isEmpty())
                .build();

        cachedPreview.set(result);
        return result;
    }

    /**
     * Re-calculates and refreshes the current cached preview if available.
     *
     * @return refreshed PreviewResult or empty result if no preview was cached
     */
    public PreviewResult refreshPreview() {
        PreviewResult current = cachedPreview.get();
        if (current == null || current.getPreview().getFiles().isEmpty()) {
            return createEmptyPreviewResult();
        }
        return generatePreview(current.getPreview().getFiles(), null);
    }

    /**
     * Clears the current cached preview.
     */
    public void clearPreview() {
        cachedPreview.set(null);
    }

    /**
     * Gets the current cached preview result.
     *
     * @return current PreviewResult or null
     */
    public PreviewResult getCachedPreview() {
        return cachedPreview.get();
    }

    private static PreviewResult createEmptyPreviewResult() {
        OrganizationPreview emptyPreview = OrganizationPreview.fromFiles(Collections.emptyList());
        return PreviewResult.builder()
                .preview(emptyPreview)
                .successful(true)
                .build();
    }
}
