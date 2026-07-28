package com.smartfolderorganizer.controller;

import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.MoveOperation;
import com.smartfolderorganizer.model.OrganizationReport;

import com.smartfolderorganizer.service.OrganizationListener;
import com.smartfolderorganizer.service.OrganizationOptions;
import com.smartfolderorganizer.service.OrganizationProgress;
import com.smartfolderorganizer.service.OrganizationService;
import com.smartfolderorganizer.service.PreviewResult;
import com.smartfolderorganizer.util.SizeFormatter;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller for the Organization Progress Workspace UI view integrated with {@link OrganizationService}.
 * <p>
 * Coordinates background execution of physical file moves via JavaFX {@link Task}, updates live progress metrics,
 * populates real-time activity audit logs, and handles execution controls.
 * </p>
 */
public class OrganizationProgressController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationProgressController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OrganizationService organizationService = new OrganizationService();
    private final ObservableList<ActivityLogEntry> activityLogList = FXCollections.observableArrayList();
    private final ObservableList<String> recentOpsList = FXCollections.observableArrayList();

    private final AtomicInteger successCounter = new AtomicInteger(0);
    private final AtomicInteger skippedCounter = new AtomicInteger(0);
    private final AtomicInteger failedCounter = new AtomicInteger(0);
    private final AtomicLong bytesMovedCounter = new AtomicLong(0);

    private Instant startTime;

    // Top Header Stats Labels
    @FXML private Label filesProcessedValueLabel;
    @FXML private Label remainingFilesValueLabel;
    @FXML private Label successfulMovesValueLabel;
    @FXML private Label failedMovesValueLabel;

    // Center Progress Section Controls
    @FXML private ProgressBar mainProgressBar;
    @FXML private Label percentageLabel;
    @FXML private Label currentFileLabel;
    @FXML private Label currentSourcePathLabel;
    @FXML private Label currentDestinationPathLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private Label elapsedTimeLabel;
    @FXML private Label transferSpeedLabel;

    // Center Bottom Live Activity TableView
    @FXML private TableView<ActivityLogEntry> liveActivityTableView;
    @FXML private TableColumn<ActivityLogEntry, String> timeColumn;
    @FXML private TableColumn<ActivityLogEntry, String> operationColumn;
    @FXML private TableColumn<ActivityLogEntry, String> fileColumn;
    @FXML private TableColumn<ActivityLogEntry, String> statusColumn;
    @FXML private TableColumn<ActivityLogEntry, String> messageColumn;

    // Right Panel Summary & Recent Operations
    @FXML private Label summaryCompletedLabel;
    @FXML private Label summarySkippedLabel;
    @FXML private Label summaryConflictsLabel;
    @FXML private Label summaryErrorsLabel;
    @FXML private ListView<String> recentOperationsListView;

    // Bottom Action Bar Controls
    @FXML private Button pauseBtn;
    @FXML private Button resumeBtn;
    @FXML private Button cancelBtn;
    @FXML private Button viewReportBtn;
    @FXML private Button openDestFolderBtn;
    @FXML private Label statusLabel;

    /**
     * Initializes the controller view state automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing OrganizationProgressController backend-integrated view...");

        setupLiveActivityTable();
        setupRecentOpsList();

        // Pause/Resume disabled as per specification (OrganizationService executes NIO moves synchronously per thread)
        if (pauseBtn != null) {
            pauseBtn.setDisable(true);
            pauseBtn.setTooltip(new javafx.scene.control.Tooltip("Pause/Resume is not supported for synchronous NIO file move operations."));
        }
        if (resumeBtn != null) {
            resumeBtn.setDisable(true);
            resumeBtn.setTooltip(new javafx.scene.control.Tooltip("Pause/Resume is not supported for synchronous NIO file move operations."));
        }

        if (statusLabel != null) statusLabel.setText("Ready");
        if (mainProgressBar != null) mainProgressBar.setProgress(0.0);

        logger.info("OrganizationProgressController initialized successfully.");
    }

    private void setupLiveActivityTable() {
        if (liveActivityTableView == null) return;

        liveActivityTableView.setItems(activityLogList);

        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime()));
        operationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOperation()));
        fileColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFile()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        messageColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));
    }

    private void setupRecentOpsList() {
        if (recentOperationsListView != null) {
            recentOperationsListView.setItems(recentOpsList);
        }
    }

    /**
     * Initiates physical file organization using a PreviewResult input.
     *
     * @param previewResult Input dry-run preview containing target files.
     * @param options       Organization configuration options.
     */
    public void startOrganization(PreviewResult previewResult, OrganizationOptions options) {
        if (previewResult == null || previewResult.getPreview().getFiles().isEmpty()) {
            showErrorAlert("Validation Error", "No preview result available to execute organization.");
            return;
        }

        OrganizationOptions execOptions = options != null ? options : OrganizationOptions.defaultOptions();

        resetProgressCounters();
        startTime = Instant.now();

        if (statusLabel != null) statusLabel.setText("Running");
        if (mainProgressBar != null) mainProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        if (cancelBtn != null) cancelBtn.setDisable(false);

        Task<OrganizationReport> orgTask = new Task<>() {
            @Override
            protected OrganizationReport call() {
                return organizationService.organize(previewResult, execOptions, new OrganizationListener() {
                    @Override
                    public void onStart() {
                        Platform.runLater(() -> {
                            if (statusLabel != null) statusLabel.setText("Running");
                        });
                    }

                    @Override
                    public void onProgress(OrganizationProgress progress) {
                        Platform.runLater(() -> updateLiveProgress(progress));
                    }

                    @Override
                    public void onFileMoved(FileItem item) {
                        int succ = successCounter.incrementAndGet();
                        bytesMovedCounter.addAndGet(item.getSize());
                        String nowTime = LocalDateTime.now().format(TIME_FORMATTER);

                        Platform.runLater(() -> {
                            if (successfulMovesValueLabel != null) successfulMovesValueLabel.setText(String.valueOf(succ));
                            if (summaryCompletedLabel != null) summaryCompletedLabel.setText(String.valueOf(succ));

                            ActivityLogEntry entry = new ActivityLogEntry(
                                    nowTime, "MOVE", item.getFileName(), "SUCCESS",
                                    "Moved -> " + (item.getDestinationPath() != null ? item.getDestinationPath().getFileName() : "")
                            );
                            activityLogList.add(0, entry);
                            recentOpsList.add(0, "✓ Moved " + item.getFileName());
                        });
                    }

                    @Override
                    public void onFileSkipped(FileItem item, String reason) {
                        int skip = skippedCounter.incrementAndGet();
                        String nowTime = LocalDateTime.now().format(TIME_FORMATTER);

                        Platform.runLater(() -> {
                            if (summarySkippedLabel != null) summarySkippedLabel.setText(String.valueOf(skip));

                            ActivityLogEntry entry = new ActivityLogEntry(
                                    nowTime, "MOVE", item != null ? item.getFileName() : "Unknown", "SKIPPED", reason
                            );
                            activityLogList.add(0, entry);
                            recentOpsList.add(0, "⚠️ Skipped " + (item != null ? item.getFileName() : "") + " (" + reason + ")");
                        });
                    }

                    @Override
                    public void onComplete(OrganizationReport report) {
                        // Handled in task succeeded listener
                    }

                    @Override
                    public void onError(Throwable error) {
                        int fail = failedCounter.incrementAndGet();
                        Platform.runLater(() -> {
                            if (failedMovesValueLabel != null) failedMovesValueLabel.setText(String.valueOf(fail));
                            if (summaryErrorsLabel != null) summaryErrorsLabel.setText(String.valueOf(fail));
                        });
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

        orgTask.setOnSucceeded(event -> {
            OrganizationReport report = orgTask.getValue();
            if (statusLabel != null) statusLabel.setText("Completed");
            if (mainProgressBar != null) mainProgressBar.setProgress(1.0);
            if (percentageLabel != null) percentageLabel.setText("100%");
            if (cancelBtn != null) cancelBtn.setDisable(true);

            long durationMs = report != null ? report.getDuration().toMillis() : 0;
            logger.info("Organization job completed successfully in {} ms. Total files organized: {}",
                    durationMs, report != null ? report.getFilesOrganized().size() : 0);
        });

        orgTask.setOnFailed(event -> {
            Throwable ex = orgTask.getException();
            logger.error("Organization job failed", ex);

            if (statusLabel != null) statusLabel.setText("Failed");
            if (mainProgressBar != null) mainProgressBar.setProgress(0.0);
            if (cancelBtn != null) cancelBtn.setDisable(true);

            showErrorAlert("Organization Failure", "An error occurred during file organization:\n" + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread worker = new Thread(orgTask, "OrganizationEngine-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateLiveProgress(OrganizationProgress progress) {
        if (progress == null) return;

        if (mainProgressBar != null) mainProgressBar.setProgress(progress.getPercentage() / 100.0);
        if (percentageLabel != null) percentageLabel.setText(String.format("%.0f%%", progress.getPercentage()));
        if (currentFileLabel != null) currentFileLabel.setText(progress.getCurrentFile());
        if (filesProcessedValueLabel != null) filesProcessedValueLabel.setText(String.valueOf(progress.getProcessedFiles()));
        if (remainingFilesValueLabel != null) remainingFilesValueLabel.setText(String.valueOf(progress.getRemainingFiles()));

        // Calculate time metrics
        if (startTime != null) {
            long elapsedSeconds = Math.max(1, java.time.Duration.between(startTime, Instant.now()).getSeconds());
            if (elapsedTimeLabel != null) elapsedTimeLabel.setText(elapsedSeconds + " s");

            long movedBytes = bytesMovedCounter.get();
            double speedBytesPerSec = (double) movedBytes / elapsedSeconds;
            if (transferSpeedLabel != null) transferSpeedLabel.setText(SizeFormatter.format((long) speedBytesPerSec) + "/s");

            long remainingFiles = progress.getRemainingFiles();
            long processedFiles = Math.max(1, progress.getProcessedFiles());
            double avgTimePerFileSec = (double) elapsedSeconds / processedFiles;
            long estimatedRemainingSec = (long) (remainingFiles * avgTimePerFileSec);
            if (timeRemainingLabel != null) timeRemainingLabel.setText(estimatedRemainingSec + " s");
        }
    }

    private void resetProgressCounters() {
        successCounter.set(0);
        skippedCounter.set(0);
        failedCounter.set(0);
        bytesMovedCounter.set(0);
        activityLogList.clear();
        recentOpsList.clear();

        if (filesProcessedValueLabel != null) filesProcessedValueLabel.setText("0");
        if (remainingFilesValueLabel != null) remainingFilesValueLabel.setText("0");
        if (successfulMovesValueLabel != null) successfulMovesValueLabel.setText("0");
        if (failedMovesValueLabel != null) failedMovesValueLabel.setText("0");
        if (summaryCompletedLabel != null) summaryCompletedLabel.setText("0");
        if (summarySkippedLabel != null) summarySkippedLabel.setText("0");
        if (summaryConflictsLabel != null) summaryConflictsLabel.setText("0");
        if (summaryErrorsLabel != null) summaryErrorsLabel.setText("0");
    }

    // Action Handlers
    @FXML
    private void onPause() {
        logger.debug("Pause action requested (Disabled for synchronous NIO moves).");
    }

    @FXML
    private void onResume() {
        logger.debug("Resume action requested (Disabled for synchronous NIO moves).");
    }

    @FXML
    private void onCancel() {
        if (organizationService.isRunning()) {
            organizationService.cancel();
            if (statusLabel != null) statusLabel.setText("Cancelling...");
            logger.info("User requested organization job cancellation.");
        }
    }

    @FXML
    private void onViewReport() {
        logger.info("View Report button clicked.");
    }

    @FXML
    private void onOpenDestinationFolder() {
        logger.info("Open Destination Folder button clicked.");
    }

    private void showErrorAlert(String header, String content) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Organization Error");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display Alert dialog", ex);
        }
    }

    /**
     * POJO helper for live activity log table rows.
     */
    public static class ActivityLogEntry {
        private final String time;
        private final String operation;
        private final String file;
        private final String status;
        private final String message;

        public ActivityLogEntry(String time, String operation, String file, String status, String message) {
            this.time = time;
            this.operation = operation;
            this.file = file;
            this.status = status;
            this.message = message;
        }

        public String getTime() { return time; }
        public String getOperation() { return operation; }
        public String getFile() { return file; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }
}
