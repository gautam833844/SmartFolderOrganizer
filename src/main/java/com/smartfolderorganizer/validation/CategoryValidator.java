package com.smartfolderorganizer.validation;

import com.smartfolderorganizer.exception.ValidationException;
import com.smartfolderorganizer.model.Category;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Production-quality validator for Category domain enum, extension mappings, custom categories, and overlap checks.
 */
public final class CategoryValidator {

    private CategoryValidator() {
        throw new UnsupportedOperationException("Utility class 'CategoryValidator' cannot be instantiated");
    }

    /**
     * Validates that a Category enum reference is non-null.
     *
     * @param category category enum
     */
    public static void validateCategory(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");
    }

    /**
     * Validates that an extension mapping is supported by a Category.
     *
     * @param category  category enum (non-null)
     * @param extension extension string (non-null)
     */
    public static void validateExtensionMapping(Category category, String extension) {
        validateCategory(category);
        FileValidator.validateExtension(extension);
        if (category != Category.OTHERS && !category.supportsExtension(extension)) {
            throw new ValidationException(String.format("Extension '%s' is not supported by category '%s'", extension, category.getDisplayName()));
        }
    }

    /**
     * Inspects built-in Category enums to ensure no duplicate extension mappings exist across distinct categories.
     */
    public static void validateBuiltInCategories() {
        Map<String, Category> seenExtensions = new HashMap<>();
        for (Category cat : Category.values()) {
            if (cat == Category.OTHERS) continue;
            for (String ext : cat.getSupportedExtensions()) {
                if (seenExtensions.containsKey(ext)) {
                    Category existingCat = seenExtensions.get(ext);
                    throw new ValidationException(String.format(
                            "Duplicate extension mapping detected: '%s' is mapped to both '%s' and '%s'",
                            ext, existingCat.getDisplayName(), cat.getDisplayName()
                    ));
                }
                seenExtensions.put(ext, cat);
            }
        }
    }

    /**
     * Validates parameters for creating a new custom category.
     *
     * @param displayName         category display name (non-null, non-blank)
     * @param folderName          category folder name (non-null, non-blank)
     * @param supportedExtensions set of file extensions
     */
    public static void validateCustomCategory(String displayName, String folderName, Set<String> supportedExtensions) {
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(folderName, "folderName must not be null");
        Objects.requireNonNull(supportedExtensions, "supportedExtensions must not be null");

        if (displayName.isBlank()) {
            throw new ValidationException("Custom category displayName cannot be blank");
        }
        if (folderName.isBlank()) {
            throw new ValidationException("Custom category folderName cannot be blank");
        }
        FileValidator.validateFileName(folderName);

        for (String ext : supportedExtensions) {
            FileValidator.validateExtension(ext);
        }
    }

    /**
     * Validates that a map of category extension rules contains no overlapping or duplicate extensions across distinct categories.
     *
     * @param categoryExtensionMap map of category to extension sets
     */
    public static void validateUniqueCategoryExtensions(Map<Category, Set<String>> categoryExtensionMap) {
        Objects.requireNonNull(categoryExtensionMap, "categoryExtensionMap must not be null");
        Map<String, Category> extensionToCategoryMap = new HashMap<>();

        categoryExtensionMap.forEach((category, extensions) -> {
            if (category != null && extensions != null && category != Category.OTHERS) {
                for (String ext : extensions) {
                    if (ext == null || ext.isBlank()) continue;
                    String cleanExt = ext.toLowerCase().trim().replaceFirst("^\\.", "");
                    if (extensionToCategoryMap.containsKey(cleanExt)) {
                        Category existingCat = extensionToCategoryMap.get(cleanExt);
                        throw new ValidationException(String.format(
                                "Duplicate extension '%s' found in categories '%s' and '%s'",
                                cleanExt, existingCat.getDisplayName(), category.getDisplayName()
                        ));
                    }
                    extensionToCategoryMap.put(cleanExt, category);
                }
            }
        });
    }
}
