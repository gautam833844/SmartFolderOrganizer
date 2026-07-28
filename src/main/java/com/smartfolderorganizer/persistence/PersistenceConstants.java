package com.smartfolderorganizer.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Constants defining file paths, default configuration values, and JSON persistence defaults.
 */
public final class PersistenceConstants {

    public static final String APP_VERSION = "1.0.0";
    public static final String APP_DIR_NAME = ".smartfolderorganizer";

    public static final String SETTINGS_FILE_NAME = "settings.json";
    public static final String RECENT_FOLDERS_FILE_NAME = "recent_folders.json";
    public static final String TRANSACTION_HISTORY_FILE_NAME = "transaction_history.json";

    private PersistenceConstants() {
        throw new UnsupportedOperationException("Utility class 'PersistenceConstants' cannot be instantiated");
    }

    /**
     * Gets the default application data directory path in the user's home directory.
     *
     * @return Path pointing to user home configuration directory
     */
    public static Path getDefaultAppDataDirectory() {
        String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, APP_DIR_NAME);
    }

    /**
     * Gets the default settings file path.
     *
     * @return Path pointing to settings.json
     */
    public static Path getDefaultSettingsFilePath() {
        return getDefaultAppDataDirectory().resolve(SETTINGS_FILE_NAME);
    }

    /**
     * Gets the default recent folders file path.
     *
     * @return Path pointing to recent_folders.json
     */
    public static Path getDefaultRecentFoldersFilePath() {
        return getDefaultAppDataDirectory().resolve(RECENT_FOLDERS_FILE_NAME);
    }

    /**
     * Gets the default transaction history file path.
     *
     * @return Path pointing to transaction_history.json
     */
    public static Path getDefaultTransactionHistoryFilePath() {
        return getDefaultAppDataDirectory().resolve(TRANSACTION_HISTORY_FILE_NAME);
    }
}
