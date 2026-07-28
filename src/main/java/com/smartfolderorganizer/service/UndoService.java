package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.MoveOperation;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service orchestrating undo operations for reversing file organization transactions.
 */
public class UndoService {

    private final TransactionHistory history;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public UndoService() {
        this(new TransactionHistory());
    }

    public UndoService(TransactionHistory history) {
        this.history = Objects.requireNonNull(history, "history must not be null");
    }

    public TransactionHistory getHistory() {
        return history;
    }

    /**
     * Reverses the most recent completed transaction in history.
     *
     * @return UndoResult summary
     */
    public UndoResult undoLatest() {
        return undoLatest(null);
    }

    /**
     * Reverses the most recent completed transaction in history with progress callbacks.
     *
     * @param listener progress listener callback (nullable)
     * @return UndoResult summary
     */
    public UndoResult undoLatest(UndoListener listener) {
        Optional<Transaction> latestOpt = history.getLatestTransaction();
        if (latestOpt.isEmpty()) {
            throw new IllegalStateException("No transactions available in history to undo.");
        }
        return undo(latestOpt.get(), listener);
    }

    /**
     * Reverses a specific transaction.
     *
     * @param transaction transaction to undo
     * @return UndoResult summary
     */
    public UndoResult undo(Transaction transaction) {
        return undo(transaction, null);
    }

    /**
     * Reverses a specific transaction with progress callbacks.
     *
     * @param transaction transaction to undo (non-null)
     * @param listener    progress listener callback (nullable)
     * @return UndoResult summary
     */
    public UndoResult undo(Transaction transaction, UndoListener listener) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        if (!transaction.isUndoable()) {
            throw new IllegalArgumentException("Transaction is not undoable or contains zero successful operations.");
        }

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("UndoService is currently executing an undo operation.");
        }

        cancelled.set(false);
        if (listener != null) {
            listener.onStart();
        }

        Instant startTime = Instant.now();
        List<MoveOperation> opsToUndo = transaction.getSuccessfulOperations();
        int totalOps = opsToUndo.size();
        int restoredCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Reverse operations order (LIFO undo)
            for (int i = opsToUndo.size() - 1; i >= 0; i--) {
                if (cancelled.get()) {
                    errors.add("Undo operation cancelled by user.");
                    if (listener != null) listener.onCancelled();
                    break;
                }

                MoveOperation op = opsToUndo.get(i);
                Path sourcePath = op.getSource();        // Original location to restore TO
                Path destPath = op.getDestination();     // Current location to restore FROM

                FileItem stubItem = FileItem.builder()
                        .originalPath(sourcePath)
                        .destinationPath(destPath)
                        .build();

                if (destPath == null || !Files.exists(destPath)) {
                    String error = "Cannot restore file; destination path missing or removed: " + destPath;
                    errors.add(error);
                    failedCount++;
                    if (listener != null) listener.onFileSkipped(stubItem, error);
                    continue;
                }

                // Ensure original parent directory exists
                Path parentDir = sourcePath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    try {
                        Files.createDirectories(parentDir);
                    } catch (IOException e) {
                        String error = "Failed to recreate original directory: " + parentDir;
                        errors.add(error);
                        failedCount++;
                        if (listener != null) listener.onFileSkipped(stubItem, error);
                        continue;
                    }
                }

                boolean restored = false;
                try {
                    performNioMove(destPath, sourcePath);
                    if (Files.exists(sourcePath)) {
                        restored = true;
                    } else {
                        errors.add("Restoration verification failed for path: " + sourcePath);
                    }
                } catch (Exception e) {
                    errors.add("Failed to move file back from " + destPath + " to " + sourcePath + ": " + e.getMessage());
                }

                if (restored) {
                    restoredCount++;
                    if (listener != null) {
                        listener.onFileRestored(stubItem);
                        listener.onProgress(restoredCount, totalOps);
                    }
                } else {
                    failedCount++;
                    if (listener != null) {
                        listener.onFileSkipped(stubItem, "Failed to move file back");
                    }
                }
            }

            Duration duration = Duration.between(startTime, Instant.now());
            boolean success = errors.isEmpty() && failedCount == 0 && !cancelled.get();

            UndoResult result = UndoResult.builder()
                    .successful(success)
                    .restoredFiles(restoredCount)
                    .failedFiles(failedCount)
                    .errors(errors)
                    .duration(duration)
                    .build();

            if (success) {
                history.removeTransaction(transaction.getTransactionId());
            }

            if (!cancelled.get() && listener != null) {
                listener.onComplete(result);
            }

            return result;

        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
            throw new RuntimeException("Undo operation failed: " + e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }

    /**
     * Cancels an ongoing undo operation.
     */
    public void cancel() {
        if (running.get()) {
            cancelled.set(true);
        }
    }

    /**
     * Checks if UndoService is currently executing an undo.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    private static void performNioMove(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicFailed) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
