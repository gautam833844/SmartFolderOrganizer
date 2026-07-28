package com.smartfolderorganizer.ui.navigation;

/**
 * Enumeration of all navigable screens within the Smart Folder Organizer application.
 * Stores screen titles and corresponding FXML resource file locations.
 */
public enum Screen {

    DASHBOARD("Dashboard", null),
    SCANNER("Scanner", "/fxml/ScannerView.fxml"),
    PREVIEW("Preview", "/fxml/PreviewView.fxml"),
    ORGANIZATION_PROGRESS("Organization Progress", "/fxml/OrganizationProgressView.fxml"),
    DUPLICATE_MANAGER("Duplicate Manager", "/fxml/DuplicateManagerView.fxml"),
    HISTORY("History", "/fxml/HistoryView.fxml"),
    REPORTS("Reports", "/fxml/ReportsView.fxml"),
    SETTINGS("Settings", "/fxml/SettingsView.fxml");

    private final String title;
    private final String fxmlPath;

    Screen(String title, String fxmlPath) {
        this.title = title;
        this.fxmlPath = fxmlPath;
    }

    /**
     * Gets the human-readable display title of the screen.
     * @return Screen title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the classpath FXML resource path for the screen view layout.
     * @return FXML resource path string, or null for built-in dashboard shell.
     */
    public String getFxmlPath() {
        return fxmlPath;
    }
}
