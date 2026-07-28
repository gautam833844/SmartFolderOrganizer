package com.smartfolderorganizer.controller;

import com.smartfolderorganizer.persistence.ApplicationSettings;
import com.smartfolderorganizer.persistence.SettingsService;
import com.smartfolderorganizer.service.OrganizationOptions;
import com.smartfolderorganizer.service.ScanOptions;
import com.smartfolderorganizer.validation.ConfigurationValidator;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Controller for the Settings & Preferences Workspace UI view integrated with {@link SettingsService} and {@link ApplicationSettings}.
 * <p>
 * Binds persistent application configuration properties, performs real-time path/theme validations,
 * supports async JSON loading/saving, and handles factory resets.
 * </p>
 */
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService = new SettingsService();

    // General Section
    @FXML private ComboBox<String> languageComboBox;
    @FXML private CheckBox autoSaveCheckBox;
    @FXML private CheckBox rememberWindowSizeCheckBox;
    @FXML private CheckBox startMaximizedCheckBox;

    // Scanning Section
    @FXML private TextField defaultScanFolderTextField;
    @FXML private Button browseScanFolderBtn;
    @FXML private CheckBox recursiveScanCheckBox;
    @FXML private CheckBox includeHiddenFilesCheckBox;
    @FXML private CheckBox followSymlinksCheckBox;
    @FXML private Spinner<Integer> maxScanThreadsSpinner;

    // Organization Section
    @FXML private TextField defaultDestFolderTextField;
    @FXML private Button browseDestFolderBtn;
    @FXML private ComboBox<String> conflictResolutionComboBox;
    @FXML private CheckBox createCategoryFoldersCheckBox;
    @FXML private CheckBox verifyMovedFilesCheckBox;

    // Duplicate Detection Section
    @FXML private ComboBox<String> hashAlgorithmComboBox;
    @FXML private Spinner<Integer> minFileSizeSpinner;
    @FXML private CheckBox parallelProcessingCheckBox;

    // Folder Watcher Section
    @FXML private CheckBox enableWatcherCheckBox;
    @FXML private CheckBox recursiveWatchCheckBox;
    @FXML private Spinner<Integer> debounceTimeSpinner;
    @FXML private CheckBox autoOrganizeNewFilesCheckBox;

    // Appearance Section
    @FXML private ComboBox<String> themeComboBox;
    @FXML private ColorPicker accentColorPicker;
    @FXML private Slider fontSizeSlider;

    // Advanced Section
    @FXML private Label appDataFolderLabel;
    @FXML private Button openAppDataFolderBtn;
    @FXML private Button exportSettingsBtn;
    @FXML private Button importSettingsBtn;
    @FXML private Button resetDefaultsBtn;

    // Right Info Panel
    @FXML private Label validationStatusLabel;
    @FXML private Label storageLocationLabel;
    @FXML private Label appVersionLabel;

    // Bottom Action Bar Controls
    @FXML private Button applyBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Button restoreDefaultsBtn;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    /**
     * Initializes the controller view state automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing SettingsController backend-integrated view...");

        setupControlRanges();
        loadSettingsFromDisk();

        logger.info("SettingsController initialized successfully.");
    }

    private void setupControlRanges() {
        if (languageComboBox != null) {
            languageComboBox.setItems(FXCollections.observableArrayList("English", "Spanish", "French", "German"));
        }
        if (conflictResolutionComboBox != null) {
            conflictResolutionComboBox.setItems(FXCollections.observableArrayList("Auto Rename", "Overwrite", "Skip", "Keep Both"));
        }
        if (hashAlgorithmComboBox != null) {
            hashAlgorithmComboBox.setItems(FXCollections.observableArrayList("MD5", "SHA-1", "SHA-256"));
        }
        if (themeComboBox != null) {
            themeComboBox.setItems(FXCollections.observableArrayList("Light", "Dark", "System"));
        }

        if (maxScanThreadsSpinner != null) {
            maxScanThreadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 4));
        }
        if (minFileSizeSpinner != null) {
            minFileSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10240, 0));
        }
        if (debounceTimeSpinner != null) {
            debounceTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 5000, 500, 100));
        }
        if (fontSizeSlider != null) {
            fontSizeSlider.setMin(10);
            fontSizeSlider.setMax(18);
            fontSizeSlider.setValue(13);
        }
    }

    /**
     * Asynchronously loads persisted JSON settings from disk and populates UI controls.
     */
    public void loadSettingsFromDisk() {
        if (statusLabel != null) statusLabel.setText("Loading settings...");
        if (progressBar != null) progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Task<ApplicationSettings> loadTask = new Task<>() {
            @Override
            protected ApplicationSettings call() {
                return settingsService.loadSettings();
            }
        };

        loadTask.setOnSucceeded(event -> {
            ApplicationSettings settings = loadTask.getValue();
            populateUIFromSettings(settings);

            if (statusLabel != null) statusLabel.setText("Settings Ready");
            if (progressBar != null) progressBar.setProgress(0.0);
            logger.info("Settings loaded successfully from disk.");
        });

        loadTask.setOnFailed(event -> {
            logger.error("Failed to load settings from disk", loadTask.getException());
            ApplicationSettings defaults = settingsService.resetSettings();
            populateUIFromSettings(defaults);

            if (statusLabel != null) statusLabel.setText("Defaults Restored (Corrupt Settings)");
            if (progressBar != null) progressBar.setProgress(0.0);
            showErrorAlert("Settings Warning", "Settings JSON was unreadable or missing. Default configurations have been restored.");
        });

        Thread thread = new Thread(loadTask, "SettingsLoader-Worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void populateUIFromSettings(ApplicationSettings settings) {
        if (settings == null) return;

        // General
        if (autoSaveCheckBox != null) autoSaveCheckBox.setSelected(settings.isAutoSave());
        if (rememberWindowSizeCheckBox != null) rememberWindowSizeCheckBox.setSelected(true);
        if (startMaximizedCheckBox != null) startMaximizedCheckBox.setSelected(false);
        if (languageComboBox != null) languageComboBox.getSelectionModel().select("English");

        // Scanning
        if (defaultScanFolderTextField != null) defaultScanFolderTextField.setText(settings.getDefaultScanFolder());
        ScanOptions scanOpt = settings.getScanOptions();
        if (scanOpt != null) {
            if (recursiveScanCheckBox != null) recursiveScanCheckBox.setSelected(scanOpt.isRecursive());
            if (includeHiddenFilesCheckBox != null) includeHiddenFilesCheckBox.setSelected(scanOpt.isIncludeHidden());
            if (followSymlinksCheckBox != null) followSymlinksCheckBox.setSelected(scanOpt.isFollowLinks());
        }

        // Organization
        if (defaultDestFolderTextField != null) defaultDestFolderTextField.setText(settings.getDefaultDestinationFolder());
        OrganizationOptions orgOpt = settings.getOrganizationOptions();
        if (orgOpt != null) {
            if (createCategoryFoldersCheckBox != null) createCategoryFoldersCheckBox.setSelected(orgOpt.isCreateDirectories());
            if (verifyMovedFilesCheckBox != null) verifyMovedFilesCheckBox.setSelected(orgOpt.isVerifyAfterMove());
            if (conflictResolutionComboBox != null) {
                conflictResolutionComboBox.getSelectionModel().select(orgOpt.isOverwriteExisting() ? "Overwrite" : "Auto Rename");
            }
        }

        // Appearance & Info
        if (themeComboBox != null) {
            String theme = settings.getTheme();
            themeComboBox.getSelectionModel().select(theme.equalsIgnoreCase("DARK") ? "Dark" : theme.equalsIgnoreCase("LIGHT") ? "Light" : "System");
        }
        if (accentColorPicker != null) accentColorPicker.setValue(Color.web("#3b82f6"));

        if (appDataFolderLabel != null) appDataFolderLabel.setText(settingsService.getSettingsFilePath().getParent().toString());
        if (storageLocationLabel != null) storageLocationLabel.setText(settingsService.getSettingsFilePath().toString());
        if (validationStatusLabel != null) validationStatusLabel.setText("Valid");
        if (appVersionLabel != null) appVersionLabel.setText("v1.0.0");
    }

    private ApplicationSettings buildSettingsFromUI() {
        String themeStr = themeComboBox != null && themeComboBox.getValue() != null ? themeComboBox.getValue().toUpperCase() : "SYSTEM";
        String scanPath = defaultScanFolderTextField != null ? defaultScanFolderTextField.getText().trim() : "";
        String destPath = defaultDestFolderTextField != null ? defaultDestFolderTextField.getText().trim() : "";
        boolean autoSave = autoSaveCheckBox != null && autoSaveCheckBox.isSelected();

        boolean recursive = recursiveScanCheckBox == null || recursiveScanCheckBox.isSelected();
        boolean hidden = includeHiddenFilesCheckBox != null && includeHiddenFilesCheckBox.isSelected();
        boolean symlinks = followSymlinksCheckBox != null && followSymlinksCheckBox.isSelected();

        ScanOptions scanOpt = ScanOptions.builder()
                .recursive(recursive)
                .includeHidden(hidden)
                .followLinks(symlinks)
                .build();

        boolean overwrite = conflictResolutionComboBox != null && "Overwrite".equals(conflictResolutionComboBox.getValue());
        boolean createDirs = createCategoryFoldersCheckBox == null || createCategoryFoldersCheckBox.isSelected();
        boolean verify = verifyMovedFilesCheckBox == null || verifyMovedFilesCheckBox.isSelected();

        OrganizationOptions orgOpt = OrganizationOptions.builder()
                .createDirectories(createDirs)
                .overwriteExisting(overwrite)
                .verifyAfterMove(verify)
                .build();

        return ApplicationSettings.builder()
                .theme(themeStr)
                .defaultScanFolder(scanPath)
                .defaultDestinationFolder(destPath)
                .scanOptions(scanOpt)
                .organizationOptions(orgOpt)
                .autoSave(autoSave)
                .build();
    }

    private boolean validateInputs() {
        try {
            String theme = themeComboBox != null ? themeComboBox.getValue() : "System";
            ConfigurationValidator.validateTheme(theme);

            String scanPathStr = defaultScanFolderTextField != null ? defaultScanFolderTextField.getText().trim() : "";
            if (!scanPathStr.isBlank() && !Files.exists(Path.of(scanPathStr))) {
                if (validationStatusLabel != null) validationStatusLabel.setText("Invalid Scan Path");
                showErrorAlert("Validation Warning", "The specified default scan directory does not exist on disk:\n" + scanPathStr);
                return false;
            }

            if (validationStatusLabel != null) validationStatusLabel.setText("Valid");
            return true;

        } catch (Exception ex) {
            if (validationStatusLabel != null) validationStatusLabel.setText("Invalid");
            showErrorAlert("Validation Error", "Invalid configuration parameters:\n" + ex.getMessage());
            return false;
        }
    }

    // Action Handlers
    @FXML
    private void onBrowseScanFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Default Scan Folder");
        Window owner = defaultScanFolderTextField != null ? defaultScanFolderTextField.getScene().getWindow() : null;
        File selected = chooser.showDialog(owner);
        if (selected != null) {
            defaultScanFolderTextField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseDestFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Default Destination Folder");
        Window owner = defaultDestFolderTextField != null ? defaultDestFolderTextField.getScene().getWindow() : null;
        File selected = chooser.showDialog(owner);
        if (selected != null) {
            defaultDestFolderTextField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onSave() {
        if (!validateInputs()) return;

        ApplicationSettings newSettings = buildSettingsFromUI();
        boolean saved = settingsService.saveSettings(newSettings);

        if (saved) {
            if (statusLabel != null) statusLabel.setText("Settings Saved");
            showInformationAlert("Settings Saved", "Application preferences have been saved to disk.");
            logger.info("Settings saved successfully.");
        } else {
            showErrorAlert("Save Error", "Failed to write settings to disk.");
        }
    }

    @FXML
    private void onApply() {
        if (!validateInputs()) return;

        ApplicationSettings newSettings = buildSettingsFromUI();
        settingsService.updateSettings(newSettings);
        if (statusLabel != null) statusLabel.setText("Settings Applied");
        logger.info("Settings applied to runtime application.");
    }

    @FXML
    private void onCancel() {
        loadSettingsFromDisk();
        if (statusLabel != null) statusLabel.setText("Changes Cancelled");
    }

    @FXML
    private void onResetDefaults() {
        onRestoreDefaults();
    }

    @FXML
    private void onRestoreDefaults() {
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Restore Factory Defaults");
        confirmAlert.setHeaderText("Reset Application Settings?");
        confirmAlert.setContentText("This will overwrite all saved paths, theme preferences, and scan options with factory defaults.\n\nAre you sure?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ApplicationSettings defaults = settingsService.resetSettings();
            populateUIFromSettings(defaults);
            if (statusLabel != null) statusLabel.setText("Factory Defaults Restored");
            logger.info("Factory default settings restored.");
        }
    }

    @FXML
    private void onOpenAppDataFolder() {
        try {
            File folder = settingsService.getSettingsFilePath().getParent().toFile();
            if (folder.exists()) {
                new ProcessBuilder("explorer.exe", folder.getAbsolutePath()).start();
            }
        } catch (Exception ex) {
            showErrorAlert("Error", "Could not open application data folder:\n" + ex.getMessage());
        }
    }

    @FXML
    private void onExportSettings() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Settings File");
        chooser.setInitialFileName("settings_backup.json");
        Window owner = defaultScanFolderTextField != null ? defaultScanFolderTextField.getScene().getWindow() : null;
        File target = chooser.showSaveDialog(owner);
        if (target != null) {
            try {
                ApplicationSettings current = buildSettingsFromUI();
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(target, current);
                showInformationAlert("Export Complete", "Settings exported successfully to:\n" + target.getAbsolutePath());
            } catch (Exception ex) {
                showErrorAlert("Export Error", "Failed to export settings:\n" + ex.getMessage());
            }
        }
    }

    @FXML
    private void onImportSettings() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Settings File");
        Window owner = defaultScanFolderTextField != null ? defaultScanFolderTextField.getScene().getWindow() : null;
        File source = chooser.showOpenDialog(owner);
        if (source != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                ApplicationSettings imported = mapper.readValue(source, ApplicationSettings.class);
                if (imported != null) {
                    settingsService.saveSettings(imported);
                    populateUIFromSettings(imported);
                    showInformationAlert("Import Complete", "Settings imported successfully.");
                }
            } catch (Exception ex) {
                showErrorAlert("Import Error", "Failed to parse imported settings file:\n" + ex.getMessage());
            }
        }
    }

    private void showErrorAlert(String header, String content) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Settings Warning");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display Alert dialog", ex);
        }
    }

    private void showInformationAlert(String header, String content) {
        try {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Settings Info");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display Alert dialog", ex);
        }
    }
}
