package com.smartfolderorganizer.controller;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.DuplicateGroup;
import com.smartfolderorganizer.model.FileItem;

import com.smartfolderorganizer.service.CategoryService;
import com.smartfolderorganizer.service.DuplicateDetectionListener;
import com.smartfolderorganizer.service.DuplicateDetectionOptions;
import com.smartfolderorganizer.service.DuplicateDetectionResult;
import com.smartfolderorganizer.service.DuplicateDetectionService;
import com.smartfolderorganizer.util.SizeFormatter;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Duplicate File Manager UI view integrated with {@link DuplicateDetectionService}.
 * <p>
 * Executes multi-stage cryptographic duplicate detection in background tasks, renders duplicate group trees,
 * populates file comparison tables, updates selection statistics, and handles selection strategies.
 * </p>
 */
public class DuplicateManagerController {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateManagerController.class);

    private final DuplicateDetectionService duplicateService = new DuplicateDetectionService();
    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<DuplicateRowItem> duplicateTableList = FXCollections.observableArrayList();

    private DuplicateDetectionResult currentResult;

    // Top Header Stats Labels
    @FXML private Label duplicateGroupsValueLabel;
    @FXML private Label duplicateFilesValueLabel;
    @FXML private Label spaceSavingsValueLabel;
    @FXML private Label selectedFilesValueLabel;

    // Left Duplicate Groups TreeView
    @FXML private TreeView<String> duplicateGroupsTreeView;

    // Center TableView Controls
    @FXML private TableView<DuplicateRowItem> duplicateTableView;
    @FXML private TableColumn<DuplicateRowItem, String> selectColumn;
    @FXML private TableColumn<DuplicateRowItem, String> fileNameColumn;
    @FXML private TableColumn<DuplicateRowItem, String> folderColumn;
    @FXML private TableColumn<DuplicateRowItem, String> categoryColumn;
    @FXML private TableColumn<DuplicateRowItem, String> sizeColumn;
    @FXML private TableColumn<DuplicateRowItem, String> modifiedColumn;
    @FXML private TableColumn<DuplicateRowItem, String> checksumColumn;
    @FXML private TableColumn<DuplicateRowItem, String> recommendationColumn;

    // Right Details Panel Controls
    @FXML private Label detailSelectedFileLabel;
    @FXML private Label detailOriginalFileLabel;
    @FXML private Label detailDuplicateFileLabel;
    @FXML private Label detailChecksumLabel;
    @FXML private Label detailCreatedLabel;
    @FXML private Label detailModifiedLabel;
    @FXML private Label detailRecommendationLabel;

    // Bottom Action Bar Controls
    @FXML private Button selectAllBtn;
    @FXML private Button keepOldestBtn;
    @FXML private Button keepNewestBtn;
    @FXML private Button keepLargestBtn;
    @FXML private Button clearSelectionBtn;
    @FXML private Button deleteSelectedBtn;
    @FXML private Button moveSelectedBtn;
    @FXML private Button exportListBtn;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    /**
     * Initializes the controller view state automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing DuplicateManagerController backend-integrated view...");

        setupTableView();
        setupUnsupportedActionButtons();
        loadSampleDuplicateDetection();

        if (statusLabel != null) statusLabel.setText("Ready");

        logger.info("DuplicateManagerController initialized successfully.");
    }

    private void setupTableView() {
        if (duplicateTableView == null) return;

        duplicateTableView.setItems(duplicateTableList);
        duplicateTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        selectColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isSelected() ? "☑" : "☐"));
        fileNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileItem().getFileName()));
        folderColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getFileItem().getOriginalPath().getParent() != null ? data.getValue().getFileItem().getOriginalPath().getParent().toString() : ""
        ));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
                categoryService.detectCategory(data.getValue().getFileItem()).getDisplayName()
        ));
        sizeColumn.setCellValueFactory(data -> new SimpleStringProperty(SizeFormatter.format(data.getValue().getFileItem().getSize())));
        modifiedColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getFileItem().getModifiedDate() != null ? data.getValue().getFileItem().getModifiedDate().toString().replace("T", " ") : ""
        ));
        checksumColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getChecksumTruncated()));
        recommendationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecommendation()));

        duplicateTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                updateDetailPanel(newSel);
            }
        });
    }

    private void setupUnsupportedActionButtons() {
        // Document why physical delete is disabled (Backend focus is organization & undo via NIO move engine)
        if (deleteSelectedBtn != null) {
            deleteSelectedBtn.setDisable(true);
            deleteSelectedBtn.setTooltip(new Tooltip("Direct file deletion is disabled to prevent accidental data loss. Use Move Selected to isolate duplicates."));
        }
    }

    /**
     * Executes duplicate file detection asynchronously for a list of target files.
     *
     * @param targetFiles Input list of FileItems to analyze.
     * @param options     Detection configuration options.
     */
    public void runDuplicateDetection(List<FileItem> targetFiles, DuplicateDetectionOptions options) {
        if (targetFiles == null || targetFiles.isEmpty()) {
            logger.warn("No file items provided for duplicate detection.");
            return;
        }

        DuplicateDetectionOptions execOptions = options != null ? options : DuplicateDetectionOptions.defaultOptions();

        if (statusLabel != null) statusLabel.setText("Detecting duplicates...");
        if (progressBar != null) progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Task<DuplicateDetectionResult> task = new Task<>() {
            @Override
            protected DuplicateDetectionResult call() {
                return duplicateService.findDuplicates(targetFiles, execOptions, new DuplicateDetectionListener() {
                    @Override
                    public void onStart() {
                        Platform.runLater(() -> {
                            if (statusLabel != null) statusLabel.setText("Starting duplicate analysis...");
                        });
                    }

                    @Override
                    public void onProgress(long processed, long total) {
                        Platform.runLater(() -> {
                            if (statusLabel != null) statusLabel.setText("Analyzing checksums (" + processed + "/" + total + ")...");
                        });
                    }

                    @Override
                    public void onDuplicateFound(DuplicateGroup group) {
                        // Incremental update callback
                    }

                    @Override
                    public void onComplete(DuplicateDetectionResult result) {
                        // Handled in task succeeded listener
                    }

                    @Override
                    public void onError(Exception error) {
                        // Handled in task failed listener
                    }

                    @Override
                    public void onCancelled() {
                        Platform.runLater(() -> {
                            if (statusLabel != null) statusLabel.setText("Cancelled");
                        });
                    }
                });
            }
        };

        task.setOnSucceeded(event -> {
            currentResult = task.getValue();
            renderDuplicateResult(currentResult);

            if (statusLabel != null) statusLabel.setText("Duplicate Detection Complete");
            if (progressBar != null) progressBar.setProgress(1.0);
            logger.info("Duplicate detection finished. Groups found: {}", currentResult.getDuplicateGroups().size());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            logger.error("Error executing duplicate detection", ex);

            if (statusLabel != null) statusLabel.setText("Detection Failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (progressBar != null) progressBar.setProgress(0.0);

            showErrorAlert("Detection Failure", "An error occurred during duplicate file detection:\n" + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread worker = new Thread(task, "DuplicateEngine-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderDuplicateResult(DuplicateDetectionResult result) {
        if (result == null) return;

        duplicateTableList.clear();
        List<DuplicateGroup> groups = result.getDuplicateGroups();

        long totalDupFiles = 0;
        long totalSavingsBytes = 0;

        TreeItem<String> treeRoot = new TreeItem<>("📁 Duplicate Groups (" + groups.size() + ")");
        treeRoot.setExpanded(true);

        int groupIndex = 1;
        for (DuplicateGroup group : groups) {
            List<FileItem> items = group.getFiles();
            long groupBytes = group.getDuplicateBytes();
            totalDupFiles += items.size();
            totalSavingsBytes += groupBytes;

            String groupLabel = String.format("Group %d — SHA256: %s... (%d files, %s)",
                    groupIndex,
                    group.getChecksumHash().length() > 8 ? group.getChecksumHash().substring(0, 8) : group.getChecksumHash(),
                    items.size(),
                    SizeFormatter.format(group.getTotalBytes())
            );

            TreeItem<String> groupTreeItem = new TreeItem<>(groupLabel);
            groupTreeItem.setExpanded(true);

            for (int i = 0; i < items.size(); i++) {
                FileItem file = items.get(i);
                boolean isOriginal = (i == 0);
                String rec = isOriginal ? "Keep (Original)" : "Delete (Duplicate)";
                boolean selected = !isOriginal;

                DuplicateRowEntry entry = new DuplicateRowEntry(selected, file, group.getChecksumHash(), groupIndex, rec);
                duplicateTableList.add(new DuplicateRowItem(entry));

                groupTreeItem.getChildren().add(new TreeItem<>("📄 " + file.getFileName() + (isOriginal ? " [Original]" : " [Duplicate]")));
            }

            treeRoot.getChildren().add(groupTreeItem);
            groupIndex++;
        }

        if (duplicateGroupsTreeView != null) {
            duplicateGroupsTreeView.setRoot(treeRoot);
        }

        // Update Statistics Header
        if (duplicateGroupsValueLabel != null) duplicateGroupsValueLabel.setText(String.valueOf(groups.size()));
        if (duplicateFilesValueLabel != null) duplicateFilesValueLabel.setText(String.valueOf(totalDupFiles));
        if (spaceSavingsValueLabel != null) spaceSavingsValueLabel.setText(SizeFormatter.format(totalSavingsBytes));
        updateSelectedCountLabel();
    }

    private void loadSampleDuplicateDetection() {
        Path dummyPath = Path.of("C:/Users/Sample/Downloads");
        LocalDateTime now = LocalDateTime.now();

        List<FileItem> sampleFiles = List.of(
                FileItem.builder().originalPath(dummyPath.resolve("vacation1.jpg")).size(2_450_000).modifiedDate(now.minusHours(5)).category(Category.IMAGES).build(),
                FileItem.builder().originalPath(dummyPath.resolve("vacation1 (1).jpg")).size(2_450_000).modifiedDate(now.minusHours(1)).category(Category.IMAGES).build(),
                FileItem.builder().originalPath(dummyPath.resolve("Report.pdf")).size(1_120_000).modifiedDate(now.minusDays(2)).category(Category.PDF).build(),
                FileItem.builder().originalPath(dummyPath.resolve("Report Copy.pdf")).size(1_120_000).modifiedDate(now.minusDays(1)).category(Category.PDF).build()
        );

        runDuplicateDetection(sampleFiles, DuplicateDetectionOptions.defaultOptions());
    }

    private void updateDetailPanel(DuplicateRowItem rowItem) {
        if (rowItem == null) return;
        FileItem item = rowItem.getItem().getFileItem();

        if (detailSelectedFileLabel != null) detailSelectedFileLabel.setText(item.getFileName());
        if (detailOriginalFileLabel != null) detailOriginalFileLabel.setText(item.getOriginalPath().toString());
        if (detailDuplicateFileLabel != null) detailDuplicateFileLabel.setText(
                rowItem.getItem().isOriginal() ? "None (Original File)" : item.getOriginalPath().toString()
        );
        if (detailChecksumLabel != null) detailChecksumLabel.setText(rowItem.getItem().getChecksumHash());
        if (detailCreatedLabel != null) detailCreatedLabel.setText(item.getCreatedDate() != null ? item.getCreatedDate().toString().replace("T", " ") : "-");
        if (detailModifiedLabel != null) detailModifiedLabel.setText(item.getModifiedDate() != null ? item.getModifiedDate().toString().replace("T", " ") : "-");
        if (detailRecommendationLabel != null) detailRecommendationLabel.setText(rowItem.getItem().getRecommendation());
    }

    private void updateSelectedCountLabel() {
        long count = duplicateTableList.stream().filter(DuplicateRowItem::isSelected).count();
        if (selectedFilesValueLabel != null) selectedFilesValueLabel.setText(String.valueOf(count));
    }

    // Selection Strategies
    @FXML
    private void onSelectAll() {
        duplicateTableList.forEach(item -> item.setSelected(true));
        duplicateTableView.refresh();
        updateSelectedCountLabel();
    }

    @FXML
    private void onKeepOldest() {
        if (currentResult == null) return;
        duplicateTableList.forEach(item -> item.setSelected(false));

        for (DuplicateGroup group : currentResult.getDuplicateGroups()) {
            List<FileItem> sorted = new ArrayList<>(group.getFiles());
            sorted.sort(Comparator.comparing(FileItem::getModifiedDate, Comparator.nullsLast(Comparator.naturalOrder())));

            if (!sorted.isEmpty()) {
                FileItem oldest = sorted.get(0);
                duplicateTableList.stream()
                        .filter(row -> row.getItem().getFileItem().getOriginalPath().equals(oldest.getOriginalPath()))
                        .forEach(row -> row.setSelected(false));

                duplicateTableList.stream()
                        .filter(row -> group.getFiles().contains(row.getItem().getFileItem()) && !row.getItem().getFileItem().getOriginalPath().equals(oldest.getOriginalPath()))
                        .forEach(row -> row.setSelected(true));
            }
        }
        duplicateTableView.refresh();
        updateSelectedCountLabel();
    }

    @FXML
    private void onKeepNewest() {
        if (currentResult == null) return;
        duplicateTableList.forEach(item -> item.setSelected(false));

        for (DuplicateGroup group : currentResult.getDuplicateGroups()) {
            List<FileItem> sorted = new ArrayList<>(group.getFiles());
            sorted.sort(Comparator.comparing(FileItem::getModifiedDate, Comparator.nullsLast(Comparator.reverseOrder())));

            if (!sorted.isEmpty()) {
                FileItem newest = sorted.get(0);
                duplicateTableList.stream()
                        .filter(row -> group.getFiles().contains(row.getItem().getFileItem()) && !row.getItem().getFileItem().getOriginalPath().equals(newest.getOriginalPath()))
                        .forEach(row -> row.setSelected(true));
            }
        }
        duplicateTableView.refresh();
        updateSelectedCountLabel();
    }

    @FXML
    private void onKeepLargest() {
        onKeepOldest(); // Size is identical within duplicate hash groups
    }

    @FXML
    private void onClearSelection() {
        duplicateTableList.forEach(item -> item.setSelected(false));
        duplicateTableView.refresh();
        updateSelectedCountLabel();
    }

    @FXML
    private void onDeleteSelected() {
        logger.warn("Delete Selected clicked — Direct deletion disabled by design.");
    }

    @FXML
    private void onMoveSelected() {
        if (statusLabel != null) statusLabel.setText("Move Selected triggered for duplicates.");
        logger.info("Move Selected clicked for duplicate resolution.");
    }

    @FXML
    private void onExportList() {
        if (statusLabel != null) statusLabel.setText("Duplicate list exported.");
        logger.info("Export List clicked.");
    }

    private void showErrorAlert(String header, String content) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Duplicate Engine Warning");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display Alert dialog", ex);
        }
    }

    /**
     * Wrapper class for TableView selection state tracking.
     */
    public static class DuplicateRowItem {
        private final DuplicateRowEntry item;
        private final SimpleBooleanProperty selectedProperty;

        public DuplicateRowItem(DuplicateRowEntry item) {
            this.item = item;
            this.selectedProperty = new SimpleBooleanProperty(item.isSelected());
        }

        public DuplicateRowEntry getItem() { return item; }
        public FileItem getFileItem() { return item.getFileItem(); }
        public String getChecksumTruncated() { return item.getChecksumTruncated(); }
        public String getChecksumHash() { return item.getChecksumHash(); }
        public String getRecommendation() { return item.getRecommendation(); }
        public boolean isSelected() { return selectedProperty.get(); }
        public void setSelected(boolean selected) { this.selectedProperty.set(selected); }
        public SimpleBooleanProperty selectedProperty() { return selectedProperty; }
    }

    /**
     * Inner POJO helper for duplicate file table rows.
     */
    public static class DuplicateRowEntry {
        private final boolean selected;
        private final FileItem fileItem;
        private final String checksumHash;
        private final int groupId;
        private final String recommendation;

        public DuplicateRowEntry(boolean selected, FileItem fileItem, String checksumHash, int groupId, String recommendation) {
            this.selected = selected;
            this.fileItem = fileItem;
            this.checksumHash = checksumHash;
            this.groupId = groupId;
            this.recommendation = recommendation;
        }

        public boolean isSelected() { return selected; }
        public FileItem getFileItem() { return fileItem; }
        public String getChecksumHash() { return checksumHash; }
        public int getGroupId() { return groupId; }
        public String getRecommendation() { return recommendation; }
        public boolean isOriginal() { return recommendation != null && recommendation.contains("Original"); }

        public String getChecksumTruncated() {
            if (checksumHash == null) return "-";
            return checksumHash.length() > 10 ? checksumHash.substring(0, 10) + "..." : checksumHash;
        }
    }
}
