package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.MoveOperation;
import com.smartfolderorganizer.model.OrganizationReport;
import com.smartfolderorganizer.model.Statistics;
import com.smartfolderorganizer.util.FileUtils;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance file organization engine executing physical file movements via Java NIO.
 * <p>
 * Supports atomic moves, directory auto-creation, post-move validation, individual error recovery,
 * move audit logs, progress tracking, and execution cancellation.
 * </p>
 */
public class OrganizationService {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Executes organization based on a PreviewResult using default options.
     *
     * @param preview preview result containing items to move
     * @return OrganizationReport execution summary
     */
    public OrganizationReport organize(PreviewResult preview) {
        return organize(preview, OrganizationOptions.defaultOptions(), null);
    }

    /**
     * Executes organization based on a PreviewResult using specific options.
     *
     * @param preview preview result containing items to move
     * @param options configuration options
     * @return OrganizationReport execution summary
     */
    public OrganizationReport organize(PreviewResult preview, OrganizationOptions options) {
        return organize(preview, options, null);
    }

    /**
     * Executes file organization with options and real-time progress listener callbacks.
     *
     * @param preview  preview result containing target items (non-null)
     * @param options  organization options (non-null)
     * @param listener progress listener (nullable)
     * @return OrganizationReport containing moved/failed items, move operations, and duration
     */
    public OrganizationReport organize(PreviewResult preview, OrganizationOptions options, OrganizationListener listener) {
        Objects.requireNonNull(preview, "preview must not be null");
        Objects.requireNonNull(options, "options must not be null");

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("OrganizationService is already executing an organization task.");
        }

        cancelled.set(false);
        if (listener != null) {
            listener.onStart();
        }

        Instant startTime = Instant.now();
        List<FileItem> allItems = preview.getPreview().getFiles();

        List<FileItem> organizedFiles = new ArrayList<>();
        List<FileItem> failedFiles = new ArrayList<>();
        List<MoveOperation> moveOperations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        long total = allItems.size();
        long processedCount = 0;

        try {
            for (FileItem item : allItems) {
                if (cancelled.get()) {
                    warnings.add("Organization cancelled by user.");
                    if (listener != null) listener.onCancelled();
                    break;
                }

                processedCount++;
                long remaining = total - processedCount;
                double percentage = total > 0 ? ((double) processedCount / total) * 100.0 : 100.0;

                if (!item.isSelected()) {
                    if (listener != null) {
                        listener.onFileSkipped(item, "File not selected");
                    }
                    continue;
                }

                Path source = item.getOriginalPath();
                Path destination = item.getDestinationPath();

                if (source == null || !Files.exists(source)) {
                    String msg = "Source file does not exist: " + source;
                    recordFailure(item, source, destination, msg, failedFiles, moveOperations, warnings, options, listener);
                    continue;
                }

                if (destination == null) {
                    String msg = "Destination path not assigned for file: " + item.getFileName();
                    recordFailure(item, source, destination, msg, failedFiles, moveOperations, warnings, options, listener);
                    continue;
                }

                if (source.equals(destination)) {
                    if (listener != null) {
                        listener.onFileSkipped(item, "Source and destination paths are identical");
                    }
                    continue;
                }

                // Handle directory creation
                Path destDir = destination.getParent();
                if (destDir != null && !Files.exists(destDir)) {
                    if (options.isCreateDirectories() && !options.isDryRun()) {
                        try {
                            Files.createDirectories(destDir);
                        } catch (IOException e) {
                            String msg = "Failed to create destination directory: " + destDir;
                            recordFailure(item, source, destination, msg, failedFiles, moveOperations, warnings, options, listener);
                            continue;
                        }
                    } else if (!options.isCreateDirectories()) {
                        String msg = "Destination directory does not exist: " + destDir;
                        recordFailure(item, source, destination, msg, failedFiles, moveOperations, warnings, options, listener);
                        continue;
                    }
                }

                // Auto-resolve duplicate target filename if overwrite is disabled
                if (Files.exists(destination) && !options.isOverwriteExisting()) {
                    destination = FileUtils.generateUniquePath(destination);
                }

                // Execute move operation
                boolean success = false;
                String opMessage = "File moved successfully";

                if (options.isDryRun()) {
                    success = true;
                    opMessage = "[Dry Run] Simulated move to " + destination;
                } else {
                    try {
                        performNioMove(source, destination, options);
                        if (options.isVerifyAfterMove() && !Files.exists(destination)) {
                            throw new IOException("Verification failed: destination file missing after move");
                        }
                        success = true;
                    } catch (Exception e) {
                        opMessage = "Move failed: " + e.getMessage();
                    }
                }

                MoveOperation op = MoveOperation.builder()
                        .source(source)
                        .destination(destination)
                        .timestamp(LocalDateTime.now())
                        .success(success)
                        .message(opMessage)
                        .build();

                moveOperations.add(op);

                if (success) {
                    FileItem updatedItem = item.toBuilder()
                            .destinationPath(destination)
                            .build();
                    organizedFiles.add(updatedItem);
                    if (listener != null) {
                        listener.onFileMoved(updatedItem);
                    }
                } else {
                    failedFiles.add(item);
                    warnings.add(opMessage);
                    if (listener != null) {
                        listener.onFileSkipped(item, opMessage);
                    }
                    if (!options.isContinueOnError()) {
                        throw new RuntimeException("Organization halted due to file move error: " + opMessage);
                    }
                }

                if (listener != null) {
                    OrganizationProgress progress = new OrganizationProgress(
                            item.getFileName(),
                            processedCount,
                            remaining,
                            percentage,
                            item.getCategory(),
                            "Processing: " + item.getFileName()
                    );
                    listener.onProgress(progress);
                }
            }

            Duration duration = Duration.between(startTime, Instant.now());
            Statistics stats = Statistics.fromFiles(organizedFiles);

            OrganizationReport report = OrganizationReport.builder()
                    .filesOrganized(organizedFiles)
                    .failedFiles(failedFiles)
                    .moveOperations(moveOperations)
                    .duration(duration)
                    .statistics(stats)
                    .warnings(warnings)
                    .executionDate(LocalDateTime.now())
                    .build();

            if (!cancelled.get() && listener != null) {
                listener.onComplete(report);
            }

            return report;

        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
            throw new RuntimeException("Organization process failed: " + e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }

    /**
     * Cancels an ongoing organization operation.
     */
    public void cancel() {
        if (running.get()) {
            cancelled.set(true);
        }
    }

    /**
     * Checks whether OrganizationService is currently executing file moves.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    private static void performNioMove(Path source, Path destination, OrganizationOptions options) throws IOException {
        List<CopyOption> copyOptionsList = new ArrayList<>();
        if (options.isOverwriteExisting()) {
            copyOptionsList.add(StandardCopyOption.REPLACE_EXISTING);
        }
        if (options.isAtomicMove()) {
            copyOptionsList.add(StandardCopyOption.ATOMIC_MOVE);
        }

        CopyOption[] copyOptions = copyOptionsList.toArray(new CopyOption[0]);

        try {
            Files.move(source, destination, copyOptions);
        } catch (IOException e) {
            // Fallback: If atomic move fails across filesystems, retry without ATOMIC_MOVE
            if (options.isAtomicMove()) {
                List<CopyOption> fallbackOptions = new ArrayList<>();
                if (options.isOverwriteExisting()) {
                    fallbackOptions.add(StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(source, destination, fallbackOptions.toArray(new CopyOption[0]));
            } else {
                throw e;
            }
        }
    }

    private void recordFailure(FileItem item, Path source, Path destination, String message,
                               List<FileItem> failedFiles, List<MoveOperation> moveOperations,
                               List<String> warnings, OrganizationOptions options,
                               OrganizationListener listener) {
        failedFiles.add(item);
        warnings.add(message);
        MoveOperation op = MoveOperation.builder()
                .source(source != null ? source : Path.of("unknown"))
                .destination(destination != null ? destination : Path.of("unknown"))
                .timestamp(LocalDateTime.now())
                .success(false)
                .message(message)
                .build();
        moveOperations.add(op);
        if (listener != null) {
            listener.onFileSkipped(item, message);
        }
        if (!options.isContinueOnError()) {
            throw new RuntimeException("Organization halted: " + message);
        }
    }
}
