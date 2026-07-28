package com.smartfolderorganizer.controller;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.service.CategoryService;
import com.smartfolderorganizer.service.ScanListener;
import com.smartfolderorganizer.service.ScanOptions;
import com.smartfolderorganizer.service.ScanProgress;
import com.smartfolderorganizer.service.ScanResult;
import com.smartfolderorganizer.service.ScanService;
import com.smartfolderorganizer.util.SizeFormatter;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Controller for the Scanner Workspace UI view integrated with {@link ScanService}.
 * <p>
 * Manages background directory scanning using JavaFX {@link Task}, populates real-time table views,
 * filters file items by category, updates item detail panels, and enforces thread safety.
 * </p>
 */
public class ScannerController {

    private static final Logger logger = LoggerFactory.getLogger(ScannerController.class);

    // Backend Services
    private final ScanService scanService = new ScanService();
    private final CategoryService categoryService = new CategoryService();

    // Data Models
    private final ObservableList<FileItem> masterFileList = FXCollections.observableArrayList();
    private FilteredList<FileItem> filteredFileList;

    // Top Toolbar Controls
    @FXML private TextField sourceFolderTextField;
    @FXML private Button browseBtn;
    @FXML private CheckBox recursiveCheckBox;
    @FXML private CheckBox hiddenFilesCheckBox;
    @FXML private Button scanBtn;
    @FXML private Button clearBtn;

    // Left Category Filter Controls
    @FXML private ToggleGroup categoryToggleGroup;
    @FXML private ToggleButton filterAllBtn;
    @FXML private ToggleButton filterImagesBtn;
    @FXML private ToggleButton filterDocumentsBtn;
    @FXML private ToggleButton filterVideosBtn;
    @FXML private ToggleButton filterAudioBtn;
    @FXML private ToggleButton filterArchivesBtn;
    @FXML private ToggleButton filterCodeBtn;
    @FXML private ToggleButton filterExecutablesBtn;
    @FXML private ToggleButton filterOthersBtn;

    // Center TableView Controls
    @FXML private TableView<FileItem> filesTableView;
    @FXML private TableColumn<FileItem, String> iconColumn;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, String> extensionColumn;
    @FXML private TableColumn<FileItem, String> categoryColumn;
    @FXML private TableColumn<FileItem, String> sizeColumn;
    @FXML private TableColumn<FileItem, String> modifiedColumn;
    @FXML private TableColumn<FileItem, String> originalPathColumn;
    @FXML private TableColumn<FileItem, String> destinationPathColumn;
    @FXML private TableColumn<FileItem, String> statusColumn;

    // Right Details Panel Controls
    @FXML private Label detailNameLabel;
    @FXML private Label detailSizeLabel;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailCreatedLabel;
    @FXML private Label detailModifiedLabel;
    @FXML private Label detailSourceLabel;
    @FXML private Label detailDestinationLabel;

    // Bottom Status Bar Controls
    @FXML private Label filesLoadedLabel;
    @FXML private Label selectedCountLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    /**
     * Initializes the controller view state automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing ScannerController backend-integrated components...");

        setupTableView();
        setupCategoryFilterListeners();
        setupSelectionListener();

        if (statusLabel != null) statusLabel.setText("Ready");
        if (progressBar != null) progressBar.setProgress(0.0);
        if (filesLoadedLabel != null) filesLoadedLabel.setText("Files Loaded: 0");
        if (selectedCountLabel != null) selectedCountLabel.setText("Selected: 0");

        logger.info("ScannerController backend integration ready.");
    }

    private void setupTableView() {
        if (filesTableView == null) return;

        filteredFileList = new FilteredList<>(masterFileList, p -> true);
        filesTableView.setItems(filteredFileList);
        filesTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        iconColumn.setCellValueFactory(data -> new SimpleStringProperty("📄"));
        fileNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileName()));
        extensionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExtension().toUpperCase()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
                categoryService.detectCategory(data.getValue()).getDisplayName()
        ));
        sizeColumn.setCellValueFactory(data -> new SimpleStringProperty(SizeFormatter.format(data.getValue().getSize())));
        modifiedColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getModifiedDate() != null ? data.getValue().getModifiedDate().toString().replace("T", " ") : ""
        ));
        originalPathColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getOriginalPath() != null ? data.getValue().getOriginalPath().toString() : ""
        ));
        destinationPathColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDestinationPath() != null ? data.getValue().getDestinationPath().toString() : ""
        ));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isDuplicate() ? "Duplicate" : "Ready"));
    }

    private void setupCategoryFilterListeners() {
        if (categoryToggleGroup == null) return;

        categoryToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                if (filterAllBtn != null) filterAllBtn.setSelected(true);
                return;
            }

            if (newVal == filterAllBtn) setFilter(p -> true);
            else if (newVal == filterImagesBtn) setFilter(p -> categoryService.detectCategory(p) == Category.IMAGES);
            else if (newVal == filterDocumentsBtn) setFilter(p -> {
                Category cat = categoryService.detectCategory(p);
                return cat == Category.DOCUMENTS || cat == Category.PDF;
            });
            else if (newVal == filterVideosBtn) setFilter(p -> categoryService.detectCategory(p) == Category.VIDEOS);
            else if (newVal == filterAudioBtn) setFilter(p -> categoryService.detectCategory(p) == Category.AUDIO);
            else if (newVal == filterArchivesBtn) setFilter(p -> categoryService.detectCategory(p) == Category.ARCHIVES);
            else if (newVal == filterCodeBtn) setFilter(p -> categoryService.detectCategory(p) == Category.CODE);
            else if (newVal == filterExecutablesBtn) setFilter(p -> categoryService.detectCategory(p) == Category.EXECUTABLES);
            else if (newVal == filterOthersBtn) setFilter(p -> categoryService.detectCategory(p) == Category.OTHERS);
        });
    }

    private void setFilter(Predicate<FileItem> predicate) {
        if (filteredFileList != null) {
            filteredFileList.setPredicate(predicate);
        }
    }

    private void setupSelectionListener() {
        if (filesTableView == null) return;

        filesTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            int selectedCount = filesTableView.getSelectionModel().getSelectedItems().size();
            if (selectedCountLabel != null) selectedCountLabel.setText("Selected: " + selectedCount);

            if (newSelection != null) {
                updateDetailPanel(newSelection);
            } else {
                clearDetailPanel();
            }
        });
    }

    private void updateDetailPanel(FileItem item) {
        Category category = categoryService.detectCategory(item);

        if (detailNameLabel != null) detailNameLabel.setText(item.getFileName());
        if (detailSizeLabel != null) detailSizeLabel.setText(SizeFormatter.format(item.getSize()));
        if (detailTypeLabel != null) detailTypeLabel.setText(item.getExtension().toUpperCase() + " File");
        if (detailCategoryLabel != null) detailCategoryLabel.setText(category.getDisplayName());
        if (detailCreatedLabel != null) detailCreatedLabel.setText(item.getCreatedDate() != null ? item.getCreatedDate().toString().replace("T", " ") : "-");
        if (detailModifiedLabel != null) detailModifiedLabel.setText(item.getModifiedDate() != null ? item.getModifiedDate().toString().replace("T", " ") : "-");
        if (detailSourceLabel != null) detailSourceLabel.setText(item.getOriginalPath() != null ? item.getOriginalPath().toString() : "-");
        if (detailDestinationLabel != null) detailDestinationLabel.setText(
                item.getDestinationPath() != null ? item.getDestinationPath().toString() : ""
        );
    }

    private void clearDetailPanel() {
        if (detailNameLabel != null) detailNameLabel.setText("-");
        if (detailSizeLabel != null) detailSizeLabel.setText("-");
        if (detailTypeLabel != null) detailTypeLabel.setText("-");
        if (detailCategoryLabel != null) detailCategoryLabel.setText("-");
        if (detailCreatedLabel != null) detailCreatedLabel.setText("-");
        if (detailModifiedLabel != null) detailModifiedLabel.setText("-");
        if (detailSourceLabel != null) detailSourceLabel.setText("-");
        if (detailDestinationLabel != null) detailDestinationLabel.setText("-");
    }

    /**
     * Handles Browse... button click to open DirectoryChooser dialog.
     */
    @FXML
    private void onBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Target Scan Folder");

        String currentPathStr = sourceFolderTextField != null ? sourceFolderTextField.getText() : null;
        if (currentPathStr != null && !currentPathStr.isBlank()) {
            File currentDir = new File(currentPathStr.trim());
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }

        Window ownerWindow = sourceFolderTextField != null ? sourceFolderTextField.getScene().getWindow() : null;
        File selectedDirectory = directoryChooser.showDialog(ownerWindow);

        if (selectedDirectory != null) {
            sourceFolderTextField.setText(selectedDirectory.getAbsolutePath());
            logger.info("Selected target directory for scanning: {}", selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Handles Scan Directory button click to initiate background ScanService execution.
     */
    @FXML
    private void onScan() {
        String pathText = sourceFolderTextField != null ? sourceFolderTextField.getText() : null;
        if (pathText == null || pathText.isBlank()) {
            showErrorAlert("Validation Error", "Please select or enter a valid directory path to scan.");
            return;
        }

        Path targetFolder = Path.of(pathText.trim());
        if (!Files.exists(targetFolder) || !Files.isDirectory(targetFolder)) {
            showErrorAlert("Directory Not Found", "The specified folder does not exist or is not a directory:\n" + targetFolder);
            return;
        }

        boolean recursive = recursiveCheckBox != null && recursiveCheckBox.isSelected();
        boolean includeHidden = hiddenFilesCheckBox != null && hiddenFilesCheckBox.isSelected();

        ScanOptions options = ScanOptions.builder()
                .recursive(recursive)
                .includeHidden(includeHidden)
                .build();

        // Prepare UI for background scan execution
        if (scanBtn != null) scanBtn.setDisable(true);
        if (clearBtn != null) clearBtn.setDisable(true);
        if (progressBar != null) progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        if (statusLabel != null) statusLabel.setText("Initializing scanner...");

        masterFileList.clear();
        clearDetailPanel();

        // Create JavaFX Task for non-blocking execution
        Task<ScanResult> scanTask = new Task<>() {
            @Override
            protected ScanResult call() {
                return scanService.scan(targetFolder, options, new ScanListener() {
                    @Override
                    public void onStart() {
                        Platform.runLater(() -> {
                            if (statusLabel != null) statusLabel.setText("Scan started...");
                        });
                    }

                    @Override
                    public void onProgress(ScanProgress progress) {
                        Platform.runLater(() -> {
                            if (statusLabel != null) {
                                statusLabel.setText("Scanning (" + progress.getFilesScanned() + " files): " + progress.getCurrentFile());
                            }
                        });
                    }

                    @Override
                    public void onComplete(ScanResult result) {
                        // Handled in task succeeded listener
                    }

                    @Override
                    public void onError(Throwable error) {
                        // Handled in task failed listener
                    }

                    @Override
                    public void onCancelled() {
                        // Handled in task cancelled listener
                    }
                });
            }
        };

        scanTask.setOnSucceeded(event -> {
            ScanResult result = scanTask.getValue();
            List<FileItem> discoveredFiles = result != null ? result.getFiles() : new ArrayList<>();

            masterFileList.setAll(discoveredFiles);
            updateCategoryCounters();

            if (statusLabel != null) {
                statusLabel.setText("Completed — Discovered " + discoveredFiles.size() + " files in " + (result != null ? result.getDuration().toMillis() : 0) + " ms");
            }
            if (filesLoadedLabel != null) filesLoadedLabel.setText("Files Loaded: " + discoveredFiles.size());
            if (progressBar != null) progressBar.setProgress(1.0);
            if (scanBtn != null) scanBtn.setDisable(false);
            if (clearBtn != null) clearBtn.setDisable(false);

            logger.info("Scan completed successfully. Total items loaded: {}", discoveredFiles.size());
        });

        scanTask.setOnFailed(event -> {
            Throwable ex = scanTask.getException();
            logger.error("Error executing background scan for directory '{}'", targetFolder, ex);

            if (statusLabel != null) statusLabel.setText("Error during scan: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (progressBar != null) progressBar.setProgress(0.0);
            if (scanBtn != null) scanBtn.setDisable(false);
            if (clearBtn != null) clearBtn.setDisable(false);

            showErrorAlert("Scan Error", "Failed to complete folder scanning:\n" + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread scanThread = new Thread(scanTask, "ScanEngine-Worker");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    /**
     * Handles Clear button click to reset input fields, master data list, and UI panels.
     */
    @FXML
    private void onClear() {
        if (scanService.isRunning()) {
            scanService.cancel();
        }

        if (sourceFolderTextField != null) sourceFolderTextField.setText("");
        masterFileList.clear();
        clearDetailPanel();
        updateCategoryCounters();

        if (statusLabel != null) statusLabel.setText("Ready");
        if (progressBar != null) progressBar.setProgress(0.0);
        if (filesLoadedLabel != null) filesLoadedLabel.setText("Files Loaded: 0");
        if (selectedCountLabel != null) selectedCountLabel.setText("Selected: 0");

        logger.info("Scanner view cleared.");
    }

    private void updateCategoryCounters() {
        long allCount = masterFileList.size();
        long imgCount = masterFileList.stream().filter(p -> categoryService.detectCategory(p) == Category.IMAGES).count();
        long docCount = masterFileList.stream().filter(p -> {
            Category cat = categoryService.detectCategory(p);
            return cat == Category.DOCUMENTS || cat == Category.PDF;
        }).count();
        long vidCount = masterFileList.stream().filter(p -> categoryService.detectCategory(p) == Category.VIDEOS).count();
        long audCount = masterFileList.stream().filter(p -> categoryService.detectCategory(p) == Category.AUDIO).count();
        long arcCount = masterFileList.stream().filter(p -> categoryService.detectCategory(p) == Category.ARCHIVES).count();
        long codCount = masterFileList.stream().filter(p -> categoryService.detectCategory(p) == Category.CODE).count();
        long exeCount = masterFileList.stream().filter(p -> categoryService.detectCategory(p) == Category.EXECUTABLES).count();
        long othCount = masterFileList.stream().filter(p -> categoryService.detectCategory(p) == Category.OTHERS).count();

        if (filterAllBtn != null) filterAllBtn.setText("All (" + allCount + ")");
        if (filterImagesBtn != null) filterImagesBtn.setText("Images (" + imgCount + ")");
        if (filterDocumentsBtn != null) filterDocumentsBtn.setText("Documents (" + docCount + ")");
        if (filterVideosBtn != null) filterVideosBtn.setText("Videos (" + vidCount + ")");
        if (filterAudioBtn != null) filterAudioBtn.setText("Audio (" + audCount + ")");
        if (filterArchivesBtn != null) filterArchivesBtn.setText("Archives (" + arcCount + ")");
        if (filterCodeBtn != null) filterCodeBtn.setText("Code (" + codCount + ")");
        if (filterExecutablesBtn != null) filterExecutablesBtn.setText("Executables (" + exeCount + ")");
        if (filterOthersBtn != null) filterOthersBtn.setText("Others (" + othCount + ")");
    }

    private void showErrorAlert(String header, String content) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Scanner Warning");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display Alert dialog", ex);
        }
    }
}
