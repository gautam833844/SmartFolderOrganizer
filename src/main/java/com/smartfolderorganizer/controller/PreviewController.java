package com.smartfolderorganizer.controller;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;

import com.smartfolderorganizer.service.ConflictDetector;
import com.smartfolderorganizer.service.PreviewResult;
import com.smartfolderorganizer.service.PreviewService;
import com.smartfolderorganizer.util.SizeFormatter;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Preview & Conflict Resolution Workspace UI view integrated with {@link PreviewService}.
 * <p>
 * Computes dry-run organization previews, populates dual source/destination TreeViews, populates
 * proposed file movement table views, displays path conflicts, and supports resolution action stubs.
 * </p>
 */
public class PreviewController {

    private static final Logger logger = LoggerFactory.getLogger(PreviewController.class);

    private final PreviewService previewService = new PreviewService();
    private final ObservableList<FileItem> previewItemsList = FXCollections.observableArrayList();

    private PreviewResult currentPreviewResult;

    // Top Header Stats Labels
    @FXML private Label totalFilesValueLabel;
    @FXML private Label filesToMoveValueLabel;
    @FXML private Label conflictsValueLabel;
    @FXML private Label spaceSavedValueLabel;

    // Center SplitPane TreeViews
    @FXML private TreeView<String> sourceTreeView;
    @FXML private TreeView<String> destinationTreeView;

    // Center TableView Controls
    @FXML private TableView<FileItem> previewTableView;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, String> originalPathColumn;
    @FXML private TableColumn<FileItem, String> destinationPathColumn;
    @FXML private TableColumn<FileItem, String> categoryColumn;
    @FXML private TableColumn<FileItem, String> actionColumn;
    @FXML private TableColumn<FileItem, String> conflictColumn;
    @FXML private TableColumn<FileItem, String> statusColumn;

    // Right Conflict Resolution Panel Controls
    @FXML private Label conflictTypeLabel;
    @FXML private Label existingFileLabel;
    @FXML private Label proposedFileLabel;
    @FXML private Label suggestedActionLabel;
    @FXML private Button autoRenameBtn;
    @FXML private Button skipFileBtn;
    @FXML private Button overwriteBtn;
    @FXML private Button keepBothBtn;

    // Bottom Status Bar Controls
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    /**
     * Initializes the controller view state automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing PreviewController backend-integrated view...");

        setupTableView();
        loadDefaultSamplePreview();

        logger.info("PreviewController initialized successfully.");
    }

    private void setupTableView() {
        if (previewTableView == null) return;

        previewTableView.setItems(previewItemsList);
        previewTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        fileNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileName()));
        originalPathColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getOriginalPath() != null ? data.getValue().getOriginalPath().getParent().toString() : ""
        ));
        destinationPathColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDestinationPath() != null ? data.getValue().getDestinationPath().toString() : ""
        ));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCategory() != null ? data.getValue().getCategory().getDisplayName() : Category.OTHERS.getDisplayName()
        ));
        actionColumn.setCellValueFactory(data -> new SimpleStringProperty("MOVE"));
        conflictColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isDuplicate() ? "Collision" : "None"
        ));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty("Ready to Move"));

        previewTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                updateConflictPanelForItem(newSel);
            }
        });
    }

    /**
     * Generates and renders a real organization preview for a list of scanned files.
     *
     * @param scannedFiles    Input list of scanned FileItems.
     * @param rootDestination Optional target root destination directory.
     */
    public void generatePreview(List<FileItem> scannedFiles, Path rootDestination) {
        if (scannedFiles == null || scannedFiles.isEmpty()) {
            logger.warn("No files provided to generate preview.");
            return;
        }

        if (statusLabel != null) statusLabel.setText("Computing preview...");
        if (progressBar != null) progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Task<PreviewResult> previewTask = new Task<>() {
            @Override
            protected PreviewResult call() {
                return previewService.generatePreview(scannedFiles, rootDestination);
            }
        };

        previewTask.setOnSucceeded(event -> {
            currentPreviewResult = previewTask.getValue();
            renderPreviewResult(currentPreviewResult);

            if (statusLabel != null) statusLabel.setText("Preview Complete");
            if (progressBar != null) progressBar.setProgress(1.0);
            logger.info("Preview generated successfully. Total files: {}", currentPreviewResult.getPreview().getTotalFiles());
        });

        previewTask.setOnFailed(event -> {
            Throwable ex = previewTask.getException();
            logger.error("Error generating organization preview", ex);

            if (statusLabel != null) statusLabel.setText("Preview Failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (progressBar != null) progressBar.setProgress(0.0);

            showErrorAlert("Preview Error", "Failed to compute organization preview:\n" + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread worker = new Thread(previewTask, "PreviewEngine-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadDefaultSamplePreview() {
        Path sourceDir = Path.of("C:/Users/Sample/Downloads");
        Path targetDir = Path.of("C:/Users/Sample/Organized");
        LocalDateTime now = LocalDateTime.now();

        List<FileItem> sampleFiles = List.of(
                FileItem.builder().originalPath(sourceDir.resolve("vacation_photo.jpg")).size(2_450_000).modifiedDate(now).category(Category.IMAGES).build(),
                FileItem.builder().originalPath(sourceDir.resolve("financial_report.pdf")).size(1_120_000).modifiedDate(now).category(Category.PDF).build(),
                FileItem.builder().originalPath(sourceDir.resolve("project_presentation.pptx")).size(5_600_000).modifiedDate(now).category(Category.DOCUMENTS).build(),
                FileItem.builder().originalPath(sourceDir.resolve("tutorial_video.mp4")).size(145_000_000).modifiedDate(now).category(Category.VIDEOS).build(),
                FileItem.builder().originalPath(sourceDir.resolve("podcast_episode.mp3")).size(12_800_000).modifiedDate(now).category(Category.AUDIO).build(),
                FileItem.builder().originalPath(sourceDir.resolve("source_backup.zip")).size(45_000_000).modifiedDate(now).category(Category.ARCHIVES).build(),
                FileItem.builder().originalPath(sourceDir.resolve("MainApplication.java")).size(4_500).modifiedDate(now).category(Category.CODE).build()
        );

        generatePreview(sampleFiles, targetDir);
    }

    private void renderPreviewResult(PreviewResult result) {
        if (result == null) return;

        List<FileItem> files = result.getPreview().getFiles();
        previewItemsList.setAll(files);

        // Update Statistics
        if (totalFilesValueLabel != null) totalFilesValueLabel.setText(String.valueOf(result.getPreview().getTotalFiles()));
        if (filesToMoveValueLabel != null) filesToMoveValueLabel.setText(String.valueOf(files.size()));
        if (conflictsValueLabel != null) conflictsValueLabel.setText(String.valueOf(result.getConflicts().size()));
        if (spaceSavedValueLabel != null) spaceSavedValueLabel.setText(SizeFormatter.format(result.getStatistics().getTotalSizeBytes()));

        // Populate Source TreeView
        if (sourceTreeView != null) {
            TreeItem<String> sourceRoot = new TreeItem<>("📁 Source Directory");
            sourceRoot.setExpanded(true);

            Map<Path, TreeItem<String>> parentNodes = new HashMap<>();
            for (FileItem item : files) {
                Path parent = item.getOriginalPath().getParent();
                TreeItem<String> dirNode = parentNodes.computeIfAbsent(parent, p -> {
                    TreeItem<String> node = new TreeItem<>("📁 " + p.getFileName());
                    node.setExpanded(true);
                    sourceRoot.getChildren().add(node);
                    return node;
                });
                dirNode.getChildren().add(new TreeItem<>("📄 " + item.getFileName()));
            }
            sourceTreeView.setRoot(sourceRoot);
        }

        // Populate Destination TreeView
        if (destinationTreeView != null) {
            TreeItem<String> destRoot = new TreeItem<>("📁 Organized Output");
            destRoot.setExpanded(true);

            Map<Category, List<FileItem>> structure = result.getFolderStructure();
            for (Category category : Category.values()) {
                List<FileItem> catFiles = structure.getOrDefault(category, List.of());
                TreeItem<String> categoryNode = new TreeItem<>("📁 " + category.getFolderName() + " (" + catFiles.size() + ")");
                categoryNode.setExpanded(true);

                for (FileItem item : catFiles) {
                    categoryNode.getChildren().add(new TreeItem<>("📄 " + item.getFileName()));
                }
                destRoot.getChildren().add(categoryNode);
            }
            destinationTreeView.setRoot(destRoot);
        }

        // Populate Conflict Resolution Panel
        if (!result.getConflicts().isEmpty()) {
            String firstConflict = result.getConflicts().get(0);
            if (conflictTypeLabel != null) conflictTypeLabel.setText("Path Collision / Warning");
            if (existingFileLabel != null) existingFileLabel.setText(firstConflict);
            if (proposedFileLabel != null) proposedFileLabel.setText("Auto-Renamed Target Path");
            if (suggestedActionLabel != null) suggestedActionLabel.setText("Suggested: Auto Rename");
        } else {
            if (conflictTypeLabel != null) conflictTypeLabel.setText("None");
            if (existingFileLabel != null) existingFileLabel.setText("-");
            if (proposedFileLabel != null) proposedFileLabel.setText("-");
            if (suggestedActionLabel != null) suggestedActionLabel.setText("All files ready for organization");
        }
    }

    private void updateConflictPanelForItem(FileItem item) {
        if (item == null) return;

        if (detailConflictCheck(item)) {
            if (conflictTypeLabel != null) conflictTypeLabel.setText("Duplicate Collision");
            if (existingFileLabel != null) existingFileLabel.setText(item.getOriginalPath().toString());
            if (proposedFileLabel != null) proposedFileLabel.setText(
                    item.getDestinationPath() != null ? item.getDestinationPath().toString() : "-"
            );
            if (suggestedActionLabel != null) suggestedActionLabel.setText("Auto Rename -> " + item.getFileName());
        } else {
            if (conflictTypeLabel != null) conflictTypeLabel.setText("No Conflict");
            if (existingFileLabel != null) existingFileLabel.setText(item.getOriginalPath().toString());
            if (proposedFileLabel != null) proposedFileLabel.setText(
                    item.getDestinationPath() != null ? item.getDestinationPath().toString() : "-"
            );
            if (suggestedActionLabel != null) suggestedActionLabel.setText("Ready to Move");
        }
    }

    private boolean detailConflictCheck(FileItem item) {
        return item.isDuplicate() || (currentPreviewResult != null && !currentPreviewResult.getConflicts().isEmpty());
    }

    // Resolution Action Stubs (Updates state without writing to disk yet)
    @FXML
    private void onAutoRename() {
        if (statusLabel != null) statusLabel.setText("Conflict Resolution: Auto Rename applied.");
        logger.info("Conflict action applied: Auto Rename");
    }

    @FXML
    private void onSkipFile() {
        if (statusLabel != null) statusLabel.setText("Conflict Resolution: Skip File applied.");
        logger.info("Conflict action applied: Skip File");
    }

    @FXML
    private void onOverwrite() {
        if (statusLabel != null) statusLabel.setText("Conflict Resolution: Overwrite applied.");
        logger.info("Conflict action applied: Overwrite");
    }

    @FXML
    private void onKeepBoth() {
        if (statusLabel != null) statusLabel.setText("Conflict Resolution: Keep Both applied.");
        logger.info("Conflict action applied: Keep Both");
    }

    private void showErrorAlert(String header, String content) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Preview Warning");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display Alert dialog", ex);
        }
    }
}
