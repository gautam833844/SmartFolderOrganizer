package com.smartfolderorganizer.controller;

import com.smartfolderorganizer.model.MoveOperation;
import com.smartfolderorganizer.persistence.TransactionPersistenceService;
import com.smartfolderorganizer.service.Transaction;
import com.smartfolderorganizer.service.TransactionHistory;
import com.smartfolderorganizer.service.UndoListener;
import com.smartfolderorganizer.service.UndoResult;
import com.smartfolderorganizer.service.UndoService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for the Transaction History Workspace UI view integrated with {@link TransactionPersistenceService} and {@link UndoService}.
 * <p>
 * Manages audit history loading/saving from disk JSON storage, detailed transaction inspections, LIFO file undo executions via JavaFX {@link Task},
 * and live restoration progress tracking.
 * </p>
 */
public class HistoryController {

    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

    private final TransactionHistory transactionHistory = new TransactionHistory();
    private final TransactionPersistenceService persistenceService = new TransactionPersistenceService();
    private final UndoService undoService = new UndoService(transactionHistory);

    private final ObservableList<TransactionSummaryEntry> transactionTableList = FXCollections.observableArrayList();
    private final ObservableList<TransactionFileRow> transactionFilesList = FXCollections.observableArrayList();

    private Transaction selectedTransaction;

    // Top Header Stats Labels
    @FXML private Label totalTransactionsValueLabel;
    @FXML private Label filesOrganizedValueLabel;
    @FXML private Label undoAvailableValueLabel;
    @FXML private Label lastRunValueLabel;

    // Left Transaction List TableView Controls
    @FXML private TableView<TransactionSummaryEntry> transactionTableView;
    @FXML private TableColumn<TransactionSummaryEntry, String> txIdColumn;
    @FXML private TableColumn<TransactionSummaryEntry, String> dateColumn;
    @FXML private TableColumn<TransactionSummaryEntry, String> filesCountColumn;
    @FXML private TableColumn<TransactionSummaryEntry, String> txStatusColumn;
    @FXML private TableColumn<TransactionSummaryEntry, String> undoAvailableColumn;

    // Center Details Panel Header Labels
    @FXML private Label detailTxIdLabel;
    @FXML private Label detailDateLabel;
    @FXML private Label detailSourceLabel;
    @FXML private Label detailDestinationLabel;
    @FXML private Label detailProcessedLabel;
    @FXML private Label detailSuccessLabel;
    @FXML private Label detailFailedLabel;
    @FXML private Label detailDurationLabel;
    @FXML private Label detailStatusLabel;

    // Center File Details TableView Controls
    @FXML private TableView<TransactionFileRow> transactionFilesTableView;
    @FXML private TableColumn<TransactionFileRow, String> fileNameColumn;
    @FXML private TableColumn<TransactionFileRow, String> originalPathColumn;
    @FXML private TableColumn<TransactionFileRow, String> destinationPathColumn;
    @FXML private TableColumn<TransactionFileRow, String> actionColumn;
    @FXML private TableColumn<TransactionFileRow, String> resultColumn;

    // Right Undo Panel Controls
    @FXML private Label undoSelectedTxLabel;
    @FXML private Label undoStatusLabel;
    @FXML private Label undoRestorableLabel;
    @FXML private Label undoWarningsLabel;
    @FXML private Button undoTxBtn;
    @FXML private Button exportTxBtn;
    @FXML private Button deleteRecordBtn;
    @FXML private Button refreshBtn;

    // Bottom Status Bar Controls
    @FXML private Label statusLabel;
    @FXML private Label selectedTxCountLabel;
    @FXML private ProgressBar progressBar;

    /**
     * Initializes the controller view state automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing HistoryController backend-integrated view...");

        setupTransactionListTable();
        setupTransactionFilesTable();
        loadHistoryFromDisk();

        if (statusLabel != null) statusLabel.setText("Ready");

        logger.info("HistoryController initialized successfully.");
    }

    private void setupTransactionListTable() {
        if (transactionTableView == null) return;

        transactionTableView.setItems(transactionTableList);
        transactionTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        txIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getShortId()));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedDate()));
        filesCountColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getFilesProcessed())));
        txStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        undoAvailableColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isUndoable() ? "Yes" : "No"));

        transactionTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedTransaction = newSel.getTransaction();
                if (selectedTxCountLabel != null) selectedTxCountLabel.setText("Selected Transactions: 1");
                updateTxDetails(selectedTransaction);
            } else {
                selectedTransaction = null;
                clearTxDetails();
            }
        });
    }

    private void setupTransactionFilesTable() {
        if (transactionFilesTableView == null) return;

        transactionFilesTableView.setItems(transactionFilesList);
        fileNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileName()));
        originalPathColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOriginalPath()));
        destinationPathColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDestinationPath()));
        actionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAction()));
        resultColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getResult()));
    }

    /**
     * Asynchronously loads stored JSON transaction history from disk.
     */
    public void loadHistoryFromDisk() {
        if (statusLabel != null) statusLabel.setText("Loading history...");
        if (progressBar != null) progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Task<Boolean> loadTask = new Task<>() {
            @Override
            protected Boolean call() {
                return persistenceService.loadTransactionHistory(transactionHistory);
            }
        };

        loadTask.setOnSucceeded(event -> {
            renderHistoryView();
            if (statusLabel != null) statusLabel.setText("History Loaded");
            if (progressBar != null) progressBar.setProgress(0.0);
        });

        loadTask.setOnFailed(event -> {
            logger.error("Failed to load transaction history from disk", loadTask.getException());
            renderHistoryView();
            if (statusLabel != null) statusLabel.setText("Ready");
            if (progressBar != null) progressBar.setProgress(0.0);
        });

        Thread thread = new Thread(loadTask, "HistoryLoader-Worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderHistoryView() {
        transactionTableList.clear();
        List<Transaction> transactions = transactionHistory.getAllTransactions();

        long totalFilesMoved = 0;
        int undoableCount = 0;
        LocalDateTime lastRun = null;

        for (Transaction tx : transactions) {
            transactionTableList.add(new TransactionSummaryEntry(tx));
            totalFilesMoved += tx.getSuccessfulOperations().size();
            if (tx.isUndoable()) undoableCount++;

            if (lastRun == null || tx.getTimestamp().isAfter(lastRun)) {
                lastRun = tx.getTimestamp();
            }
        }

        // Header Stats
        if (totalTransactionsValueLabel != null) totalTransactionsValueLabel.setText(String.valueOf(transactions.size()));
        if (filesOrganizedValueLabel != null) filesOrganizedValueLabel.setText(String.valueOf(totalFilesMoved));
        if (undoAvailableValueLabel != null) undoAvailableValueLabel.setText(String.valueOf(undoableCount));
        if (lastRunValueLabel != null) lastRunValueLabel.setText(lastRun != null ? lastRun.toString().replace("T", " ") : "-");

        if (!transactionTableList.isEmpty() && transactionTableView != null) {
            transactionTableView.getSelectionModel().select(0);
        } else {
            clearTxDetails();
        }
    }

    private void updateTxDetails(Transaction tx) {
        if (tx == null) {
            clearTxDetails();
            return;
        }

        if (detailTxIdLabel != null) detailTxIdLabel.setText(tx.getTransactionId().toString().substring(0, 8));
        if (detailDateLabel != null) detailDateLabel.setText(tx.getTimestamp().toString().replace("T", " "));
        if (detailProcessedLabel != null) detailProcessedLabel.setText(String.valueOf(tx.getOperations().size()));
        if (detailSuccessLabel != null) detailSuccessLabel.setText(String.valueOf(tx.getSuccessfulOperations().size()));
        if (detailFailedLabel != null) detailFailedLabel.setText(String.valueOf(tx.getFailedOperations().size()));
        if (detailDurationLabel != null) detailDurationLabel.setText("Completed");
        if (detailStatusLabel != null) detailStatusLabel.setText(tx.isCompleted() ? "Completed" : "Failed");

        // Infer Source and Destination Paths from operations
        List<MoveOperation> ops = tx.getOperations();
        if (!ops.isEmpty()) {
            Path firstSource = ops.get(0).getSource();
            Path firstDest = ops.get(0).getDestination();
            if (detailSourceLabel != null) detailSourceLabel.setText(firstSource != null && firstSource.getParent() != null ? firstSource.getParent().toString() : "-");
            if (detailDestinationLabel != null) detailDestinationLabel.setText(firstDest != null && firstDest.getParent() != null ? firstDest.getParent().toString() : "-");
        }

        // Populate File Rows
        transactionFilesList.clear();
        for (MoveOperation op : ops) {
            String fileName = op.getSource() != null && op.getSource().getFileName() != null ? op.getSource().getFileName().toString() : "Unknown";
            String srcStr = op.getSource() != null ? op.getSource().toString() : "";
            String destStr = op.getDestination() != null ? op.getDestination().toString() : "";
            String res = op.isSuccess() ? "SUCCESS" : "FAILED";
            transactionFilesList.add(new TransactionFileRow(fileName, srcStr, destStr, "MOVE", res));
        }

        // Update Right Undo Panel
        if (undoSelectedTxLabel != null) undoSelectedTxLabel.setText(tx.getTransactionId().toString().substring(0, 8));
        if (undoStatusLabel != null) undoStatusLabel.setText(tx.isUndoable() ? "Ready to Undo" : "Undo Unavailable");
        if (undoRestorableLabel != null) undoRestorableLabel.setText(tx.getSuccessfulOperations().size() + " / " + tx.getOperations().size() + " files");
        if (undoWarningsLabel != null) undoWarningsLabel.setText(tx.getFailedOperations().isEmpty() ? "No warnings" : tx.getFailedOperations().size() + " operations failed");

        if (undoTxBtn != null) undoTxBtn.setDisable(!tx.isUndoable());
    }

    private void clearTxDetails() {
        if (detailTxIdLabel != null) detailTxIdLabel.setText("-");
        if (detailDateLabel != null) detailDateLabel.setText("-");
        if (detailSourceLabel != null) detailSourceLabel.setText("-");
        if (detailDestinationLabel != null) detailDestinationLabel.setText("-");
        if (detailProcessedLabel != null) detailProcessedLabel.setText("0");
        if (detailSuccessLabel != null) detailSuccessLabel.setText("0");
        if (detailFailedLabel != null) detailFailedLabel.setText("0");
        if (detailDurationLabel != null) detailDurationLabel.setText("-");
        if (detailStatusLabel != null) detailStatusLabel.setText("-");

        transactionFilesList.clear();

        if (undoSelectedTxLabel != null) undoSelectedTxLabel.setText("-");
        if (undoStatusLabel != null) undoStatusLabel.setText("-");
        if (undoRestorableLabel != null) undoRestorableLabel.setText("-");
        if (undoWarningsLabel != null) undoWarningsLabel.setText("-");
        if (undoTxBtn != null) undoTxBtn.setDisable(true);
    }

    /**
     * Executes LIFO file undo restoration for the currently selected transaction.
     */
    @FXML
    private void onUndoTransaction() {
        if (selectedTransaction == null || !selectedTransaction.isUndoable()) {
            showErrorAlert("Undo Unavailable", "The selected transaction cannot be undone.");
            return;
        }

        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Undo Operation");
        confirmAlert.setHeaderText("Undo Transaction " + selectedTransaction.getTransactionId().toString().substring(0, 8));
        confirmAlert.setContentText("This will move " + selectedTransaction.getSuccessfulOperations().size() + " organized files back to their original paths.\n\nAre you sure you want to proceed?");

        Optional<ButtonType> response = confirmAlert.showAndWait();
        if (response.isEmpty() || response.get() != ButtonType.OK) {
            logger.info("Undo operation cancelled by user confirmation.");
            return;
        }

        if (statusLabel != null) statusLabel.setText("Restoring files...");
        if (progressBar != null) progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        if (undoTxBtn != null) undoTxBtn.setDisable(true);

        Task<UndoResult> undoTask = new Task<>() {
            @Override
            protected UndoResult call() {
                return undoService.undo(selectedTransaction, new UndoListener() {
                    @Override
                    public void onStart() {
                        Platform.runLater(() -> {
                            if (statusLabel != null) statusLabel.setText("Restoration started...");
                        });
                    }

                    @Override
                    public void onProgress(long restored, long total) {
                        Platform.runLater(() -> {
                            if (statusLabel != null) statusLabel.setText("Restoring (" + restored + "/" + total + ")...");
                        });
                    }

                    @Override
                    public void onFileRestored(com.smartfolderorganizer.model.FileItem item) {
                        // Incremental update callback
                    }

                    @Override
                    public void onFileSkipped(com.smartfolderorganizer.model.FileItem item, String reason) {
                        // Incremental update callback
                    }

                    @Override
                    public void onComplete(UndoResult result) {
                        // Handled in task succeeded listener
                    }

                    @Override
                    public void onError(Throwable error) {
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

        undoTask.setOnSucceeded(event -> {
            UndoResult result = undoTask.getValue();
            persistenceService.saveTransactionHistory(transactionHistory);
            renderHistoryView();

            if (statusLabel != null) statusLabel.setText("Undo Complete");
            if (progressBar != null) progressBar.setProgress(1.0);

            showInformationAlert("Undo Complete", "Successfully restored " + result.getRestoredFiles() + " files to their original directories.\nFailed: " + result.getFailedFiles());
            logger.info("Undo completed successfully. Restored: {}, Failed: {}", result.getRestoredFiles(), result.getFailedFiles());
        });

        undoTask.setOnFailed(event -> {
            Throwable ex = undoTask.getException();
            logger.error("Error executing undo operation", ex);

            if (statusLabel != null) statusLabel.setText("Undo Failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (progressBar != null) progressBar.setProgress(0.0);

            showErrorAlert("Undo Error", "Failed to complete transaction restoration:\n" + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread worker = new Thread(undoTask, "UndoEngine-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onExportTransaction() {
        if (selectedTransaction == null) {
            showErrorAlert("Export Warning", "Please select a transaction to export.");
            return;
        }

        boolean exportOk = persistenceService.exportHistory(transactionHistory, persistenceService.getHistoryFilePath());
        if (exportOk) {
            showInformationAlert("Export Successful", "Transaction history successfully exported to:\n" + persistenceService.getHistoryFilePath());
            if (statusLabel != null) statusLabel.setText("History exported");
        } else {
            showErrorAlert("Export Failed", "Could not write transaction history to disk.");
        }
    }

    @FXML
    private void onDeleteRecord() {
        if (selectedTransaction == null) return;

        transactionHistory.removeTransaction(selectedTransaction.getTransactionId());
        persistenceService.saveTransactionHistory(transactionHistory);
        renderHistoryView();

        if (statusLabel != null) statusLabel.setText("Record deleted");
        logger.info("Deleted transaction record {}", selectedTransaction.getTransactionId());
    }

    @FXML
    private void onRefresh() {
        loadHistoryFromDisk();
    }

    private void showErrorAlert(String header, String content) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("History Warning");
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
            alert.setTitle("History Info");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display Alert dialog", ex);
        }
    }

    /**
     * Inner POJO helper for transaction summary table rows.
     */
    public static class TransactionSummaryEntry {
        private final Transaction transaction;

        public TransactionSummaryEntry(Transaction transaction) {
            this.transaction = transaction;
        }

        public Transaction getTransaction() { return transaction; }
        public String getShortId() { return transaction.getTransactionId().toString().substring(0, 8); }
        public String getFormattedDate() { return transaction.getTimestamp().toString().replace("T", " "); }
        public int getFilesProcessed() { return transaction.getOperations().size(); }
        public String getStatus() { return transaction.isCompleted() ? "Completed" : "Failed"; }
        public boolean isUndoable() { return transaction.isUndoable(); }
    }

    /**
     * Inner POJO helper for transaction file detail rows.
     */
    public static class TransactionFileRow {
        private final String fileName;
        private final String originalPath;
        private final String destinationPath;
        private final String action;
        private final String result;

        public TransactionFileRow(String fileName, String originalPath, String destinationPath, String action, String result) {
            this.fileName = fileName;
            this.originalPath = originalPath;
            this.destinationPath = destinationPath;
            this.action = action;
            this.result = result;
        }

        public String getFileName() { return fileName; }
        public String getOriginalPath() { return originalPath; }
        public String getDestinationPath() { return destinationPath; }
        public String getAction() { return action; }
        public String getResult() { return result; }
    }
}
