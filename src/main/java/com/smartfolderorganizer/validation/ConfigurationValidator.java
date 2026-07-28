package com.smartfolderorganizer.validation;

import com.smartfolderorganizer.exception.ValidationException;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Production-quality validator for application configuration properties, UI theme settings, window dimensions,
 * and default folder configurations.
 */
public final class ConfigurationValidator {

    private static final List<String> VALID_THEMES = List.of("LIGHT", "DARK", "SYSTEM");

    private ConfigurationValidator() {
        throw new UnsupportedOperationException("Utility class 'ConfigurationValidator' cannot be instantiated");
    }

    /**
     * Validates UI theme string.
     *
     * @param theme theme string (e.g. "LIGHT", "DARK", "SYSTEM")
     */
    public static void validateTheme(String theme) {
        Objects.requireNonNull(theme, "theme must not be null");
        if (!VALID_THEMES.contains(theme.trim().toUpperCase(Locale.US))) {
            throw new ValidationException(String.format("Invalid theme '%s'. Supported themes: %s", theme, VALID_THEMES));
        }
    }

    /**
     * Validates application window bounds.
     *
     * @param width  window width in pixels
     * @param height window height in pixels
     */
    public static void validateWindowSize(double width, double height) {
        if (width < 800.0) {
            throw new ValidationException("Window width cannot be less than 800.0px: " + width);
        }
        if (height < 600.0) {
            throw new ValidationException("Window height cannot be less than 600.0px: " + height);
        }
    }

    /**
     * Validates default folder path specified in settings.
     *
     * @param defaultFolder folder path
     */
    public static void validateDefaultFolder(Path defaultFolder) {
        if (defaultFolder != null) {
            PathValidator.validateIsDirectory(defaultFolder);
            PathValidator.validateReadable(defaultFolder);
        }
    }

    /**
     * Validates a generic key-value configuration pair.
     *
     * @param key   config key
     * @param value config value
     */
    public static void validateProperty(String key, String value) {
        Objects.requireNonNull(key, "property key must not be null");
        if (key.isBlank()) {
            throw new ValidationException("Property key cannot be blank");
        }
        if (value == null) {
            throw new ValidationException("Property value for key '" + key + "' cannot be null");
        }
    }

    /**
     * Validates an entire Properties object.
     *
     * @param properties properties instance
     */
    public static void validateProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        properties.forEach((k, v) -> validateProperty(String.valueOf(k), String.valueOf(v)));
    }
}
