package com.smartfolderorganizer.controller;

import com.smartfolderorganizer.service.FolderWatcherManager;
import com.smartfolderorganizer.ui.navigation.NavigationManager;
import com.smartfolderorganizer.ui.navigation.Screen;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Main Dashboard UI shell.
 * <p>
 * Binds navigation controls, initializes global NavigationManager, binds status indicators,
 * and coordinates lifecycle tasks with {@link FolderWatcherManager}.
 * </p>
 */
public class MainDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(MainDashboardController.class);

    @FXML private BorderPane mainBorderPane;

    // Top Toolbar Buttons
    @FXML private Button scanFolderBtn;
    @FXML private Button previewBtn;
    @FXML private Button organizeBtn;
    @FXML private Button undoBtn;
    @FXML private Button duplicatesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button historyBtn;
    @FXML private Button settingsBtn;

    // Left Navigation Buttons
    @FXML private Button navDashboardBtn;
    @FXML private Button navScannerBtn;
    @FXML private Button navPreviewBtn;
    @FXML private Button navDuplicatesBtn;
    @FXML private Button navHistoryBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navSettingsBtn;

    // Center Cards Stat Labels
    @FXML private Label filesScannedValueLabel;
    @FXML private Label duplicatesValueLabel;
    @FXML private Label spaceSavedValueLabel;
    @FXML private Label transactionsValueLabel;

    // Right Panel Activity List
    @FXML private ListView<String> recentActivityListView;

    // Bottom Status Bar Elements
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label versionLabel;

    /**
     * Initializes the controller view state automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing MainDashboardController view components...");

        // Capture default dashboard center node and initialize NavigationManager
        if (mainBorderPane != null) {
            Node defaultCenterNode = mainBorderPane.getCenter();
            NavigationManager.getInstance().initialize(mainBorderPane, defaultCenterNode);
        }

        setupNavigationHandlers();
        populatePlaceholderData();
        setupWatcherBinding();

        logger.info("MainDashboardController initialized successfully.");
    }

    private void setupNavigationHandlers() {
        NavigationManager nav = NavigationManager.getInstance();

        // Left Navigation Bar Connections
        if (navDashboardBtn != null) navDashboardBtn.setOnAction(e -> nav.navigate(Screen.DASHBOARD));
        if (navScannerBtn != null) navScannerBtn.setOnAction(e -> nav.navigate(Screen.SCANNER));
        if (navPreviewBtn != null) navPreviewBtn.setOnAction(e -> nav.navigate(Screen.PREVIEW));
        if (navDuplicatesBtn != null) navDuplicatesBtn.setOnAction(e -> nav.navigate(Screen.DUPLICATE_MANAGER));
        if (navHistoryBtn != null) navHistoryBtn.setOnAction(e -> nav.navigate(Screen.HISTORY));
        if (navReportsBtn != null) navReportsBtn.setOnAction(e -> nav.navigate(Screen.REPORTS));
        if (navSettingsBtn != null) navSettingsBtn.setOnAction(e -> nav.navigate(Screen.SETTINGS));

        // Top Action Toolbar Connections
        if (scanFolderBtn != null) scanFolderBtn.setOnAction(e -> nav.navigate(Screen.SCANNER));
        if (previewBtn != null) previewBtn.setOnAction(e -> nav.navigate(Screen.PREVIEW));
        if (organizeBtn != null) organizeBtn.setOnAction(e -> nav.navigate(Screen.ORGANIZATION_PROGRESS));
        if (undoBtn != null) undoBtn.setOnAction(e -> nav.navigate(Screen.HISTORY));
        if (duplicatesBtn != null) duplicatesBtn.setOnAction(e -> nav.navigate(Screen.DUPLICATE_MANAGER));
        if (reportsBtn != null) reportsBtn.setOnAction(e -> nav.navigate(Screen.REPORTS));
        if (historyBtn != null) historyBtn.setOnAction(e -> nav.navigate(Screen.HISTORY));
        if (settingsBtn != null) settingsBtn.setOnAction(e -> nav.navigate(Screen.SETTINGS));
    }

    private void setupWatcherBinding() {
        FolderWatcherManager manager = FolderWatcherManager.getInstance();
        manager.watcherStatusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (statusLabel != null && newStatus != null) {
                statusLabel.setText("Watcher: " + newStatus);
            }
        });

        // Initialize folder watcher on startup using settings
        manager.initializeFromSettings();
    }

    private void populatePlaceholderData() {
        if (filesScannedValueLabel != null) filesScannedValueLabel.setText("12,450");
        if (duplicatesValueLabel != null) duplicatesValueLabel.setText("342");
        if (spaceSavedValueLabel != null) spaceSavedValueLabel.setText("4.2 GB");
        if (transactionsValueLabel != null) transactionsValueLabel.setText("28");

        if (recentActivityListView != null) {
            ObservableList<String> placeholderActivities = FXCollections.observableArrayList(
                    "Organized Downloads folder — 145 files moved",
                    "Scanned Documents directory — 3 duplicates found",
                    "Undo operation completed — 12 files restored",
                    "Cleaned Archives folder — 1.2 GB space saved",
                    "Folder Watcher started on configured directory",
                    "Exported transaction audit history report"
            );
            recentActivityListView.setItems(placeholderActivities);
        }

        if (statusLabel != null) statusLabel.setText("Ready");
        if (progressBar != null) progressBar.setProgress(0.0);
        if (versionLabel != null) versionLabel.setText("v1.0.0");
    }
}
