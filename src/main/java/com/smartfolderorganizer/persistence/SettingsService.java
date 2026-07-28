package com.smartfolderorganizer.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe service responsible for loading, persisting, and resetting {@link ApplicationSettings} using Jackson JSON serialization.
 */
public class SettingsService {

    private final Path settingsFilePath;
    private final ObjectMapper mapper;
    private final AtomicReference<ApplicationSettings> currentSettings = new AtomicReference<>();

    public SettingsService() {
        this(PersistenceConstants.getDefaultSettingsFilePath());
    }

    public SettingsService(Path settingsFilePath) {
        this.settingsFilePath = Objects.requireNonNull(settingsFilePath, "settingsFilePath must not be null");
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        loadSettings();
    }

    /**
     * Gets the current cached application settings.
     *
     * @return non-null ApplicationSettings
     */
    public ApplicationSettings getSettings() {
        ApplicationSettings settings = currentSettings.get();
        if (settings == null) {
            return ApplicationSettings.defaultSettings();
        }
        return settings;
    }

    /**
     * Loads settings from JSON file. Falls back to default settings if file is missing or corrupted.
     *
     * @return loaded ApplicationSettings
     */
    public synchronized ApplicationSettings loadSettings() {
        if (!Files.exists(settingsFilePath)) {
            ApplicationSettings defaults = ApplicationSettings.defaultSettings();
            currentSettings.set(defaults);
            return defaults;
        }

        try {
            ApplicationSettings loaded = mapper.readValue(settingsFilePath.toFile(), ApplicationSettings.class);
            if (loaded != null) {
                currentSettings.set(loaded);
                return loaded;
            }
        } catch (IOException ignored) {
            // Graceful recovery on corrupted or unreadable JSON file
        }

        ApplicationSettings fallback = ApplicationSettings.defaultSettings();
        currentSettings.set(fallback);
        return fallback;
    }

    /**
     * Persists settings object to JSON file.
     *
     * @param settings settings to save
     * @return true if save succeeded
     */
    public synchronized boolean saveSettings(ApplicationSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        try {
            Path parentDir = settingsFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            mapper.writeValue(settingsFilePath.toFile(), settings);
            currentSettings.set(settings);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Updates current settings and automatically saves if autoSave is enabled.
     *
     * @param settings updated settings
     * @return true if update and optional save succeeded
     */
    public synchronized boolean updateSettings(ApplicationSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        currentSettings.set(settings);
        if (settings.isAutoSave()) {
            return saveSettings(settings);
        }
        return true;
    }

    /**
     * Resets application settings to default values and saves to file.
     *
     * @return default ApplicationSettings
     */
    public synchronized ApplicationSettings resetSettings() {
        ApplicationSettings defaults = ApplicationSettings.defaultSettings();
        saveSettings(defaults);
        return defaults;
    }

    public Path getSettingsFilePath() {
        return settingsFilePath;
    }
}
