package com.smartfolderorganizer.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SettingsService Automated Unit Tests")
class SettingsServiceTest {

    @Test
    @DisplayName("Should return default application settings if file does not exist")
    void shouldReturnDefaultSettingsWhenMissing(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("non_existent_settings.json");
        SettingsService service = new SettingsService(settingsFile);

        ApplicationSettings settings = service.getSettings();
        assertNotNull(settings);
        assertEquals("SYSTEM", settings.getTheme());
        assertTrue(settings.isAutoSave());
    }

    @Test
    @DisplayName("Should save and reload custom application settings")
    void shouldSaveAndReloadCustomSettings(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("settings.json");
        SettingsService service = new SettingsService(settingsFile);

        ApplicationSettings custom = ApplicationSettings.builder()
                .theme("DARK")
                .defaultScanFolder("C:/Downloads")
                .autoSave(false)
                .build();

        boolean saved = service.saveSettings(custom);
        assertTrue(saved);

        ApplicationSettings reloaded = service.loadSettings();
        assertEquals("DARK", reloaded.getTheme());
        assertEquals("C:/Downloads", reloaded.getDefaultScanFolder());
        assertFalse(reloaded.isAutoSave());
    }

    @Test
    @DisplayName("Should recover and return factory defaults on corrupted settings JSON")
    void shouldRecoverFromCorruptSettingsJson(@TempDir Path tempDir) throws IOException {
        Path settingsFile = tempDir.resolve("corrupt_settings.json");
        Files.writeString(settingsFile, "{{ CORRUPTED_SETTINGS... }}");

        SettingsService service = new SettingsService(settingsFile);
        ApplicationSettings settings = service.loadSettings();

        assertNotNull(settings);
        assertEquals("SYSTEM", settings.getTheme()); // Fallback default
    }
}
