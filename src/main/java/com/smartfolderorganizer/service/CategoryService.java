package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.util.FileUtils;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Service orchestrating file category detection using registered extension rules and MIME probing.
 * <p>
 * Evaluates rules from {@link CategoryRegistry} first and falls back to {@link MimeTypeDetector} and
 * default enum lookups, guaranteeing a non-null {@link Category} outcome (defaulting to {@link Category#OTHERS}).
 * </p>
 */
public class CategoryService {

    private final CategoryRegistry registry;

    public CategoryService() {
        this(new CategoryRegistry());
    }

    public CategoryService(CategoryRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "CategoryRegistry must not be null");
    }

    /**
     * Gets the associated CategoryRegistry.
     *
     * @return non-null CategoryRegistry
     */
    public CategoryRegistry getRegistry() {
        return registry;
    }

    /**
     * Detects Category from extension string.
     *
     * @param extension file extension (e.g., "png", ".pdf")
     * @return detected Category or {@link Category#OTHERS}
     */
    public Category detectCategory(String extension) {
        if (extension == null || extension.isBlank()) {
            return Category.OTHERS;
        }

        Optional<CategoryRule> ruleMatch = registry.findRuleForExtension(extension);
        if (ruleMatch.isPresent()) {
            return ruleMatch.get().getCategory();
        }

        return Category.fromExtension(extension);
    }

    /**
     * Detects Category for a FileItem entity.
     *
     * @param file target FileItem
     * @return detected Category
     */
    public Category detectCategory(FileItem file) {
        Objects.requireNonNull(file, "file must not be null");
        if (file.getCategory() != null && file.getCategory() != Category.OTHERS) {
            return file.getCategory();
        }
        return detectCategory(file.getOriginalPath(), true);
    }

    /**
     * Detects Category for a file system Path using both extension matching and MIME probing.
     *
     * @param path target path
     * @return detected Category
     */
    public Category detectCategory(Path path) {
        return detectCategory(path, true);
    }

    /**
     * Detects Category for a file system Path with optional MIME probing.
     *
     * @param path        target path
     * @param useMimeType whether to attempt MIME probing if extension matching yields OTHERS
     * @return detected Category
     */
    public Category detectCategory(Path path, boolean useMimeType) {
        if (path == null) {
            return Category.OTHERS;
        }

        String extension = FileUtils.getExtension(path);
        Category fromExt = detectCategory(extension);

        if (fromExt != Category.OTHERS) {
            return fromExt;
        }

        if (useMimeType) {
            Optional<Category> mimeCategory = MimeTypeDetector.detectCategory(path);
            if (mimeCategory.isPresent()) {
                return mimeCategory.get();
            }
        }

        return Category.OTHERS;
    }
}
