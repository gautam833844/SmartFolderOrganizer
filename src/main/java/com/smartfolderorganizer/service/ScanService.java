package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.Statistics;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance, non-blocking file system directory scanning engine built on Java NIO FileVisitor.
 * <p>
 * Collects file metadata without reading full file content, gracefully tolerates inaccessible files/directories,
 * and supports progress tracking and cancellation.
 * </p>
 */
public class ScanService {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final AtomicLong filesScannedCounter = new AtomicLong(0);
    private final AtomicLong directoriesScannedCounter = new AtomicLong(0);
    private final AtomicLong skippedFilesCounter = new AtomicLong(0);

    private volatile String currentFileString = "";
    private volatile String currentStatusString = "Idle";

    /**
     * Scans a target directory using default scan options.
     *
     * @param folder directory path to scan
     * @return aggregated ScanResult
     */
    public ScanResult scan(Path folder) {
        return scan(folder, ScanOptions.defaultOptions(), null);
    }

    /**
     * Scans a target directory using specified options.
     *
     * @param folder  directory path to scan
     * @param options scanning configuration options
     * @return aggregated ScanResult
     */
    public ScanResult scan(Path folder, ScanOptions options) {
        return scan(folder, options, null);
    }

    /**
     * Executes directory scanning with full options and real-time progress callbacks.
     *
     * @param folder   directory path to scan (non-null)
     * @param options  scan options (non-null)
     * @param listener progress listener callback (nullable)
     * @return aggregated ScanResult
     */
    public ScanResult scan(Path folder, ScanOptions options, ScanListener listener) {
        Objects.requireNonNull(folder, "folder path must not be null");
        Objects.requireNonNull(options, "options must not be null");

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("ScanService is already executing a scan operation.");
        }

        cancelled.set(false);
        filesScannedCounter.set(0);
        directoriesScannedCounter.set(0);
        skippedFilesCounter.set(0);
        currentFileString = folder.toString();
        currentStatusString = "Initializing scan...";

        if (listener != null) {
            listener.onStart();
        }

        Instant startTime = Instant.now();
        List<FileItem> collectedFiles = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = new CopyOnWriteArrayList<>();

        try {
            if (!Files.exists(folder) || !Files.isDirectory(folder)) {
                throw new IllegalArgumentException("Target scan path must be an existing directory: " + folder);
            }

            EnumSet<FileVisitOption> visitOptions = options.isFollowLinks()
                    ? EnumSet.of(FileVisitOption.FOLLOW_LINKS)
                    : EnumSet.noneOf(FileVisitOption.class);

            int maxDepth = options.isRecursive() ? options.getMaximumDepth() : 1;

            Files.walkFileTree(folder, visitOptions, maxDepth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (cancelled.get()) {
                        return FileVisitResult.TERMINATE;
                    }

                    if (!options.isIncludeHidden() && isHiddenPath(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    directoriesScannedCounter.incrementAndGet();
                    updateProgress(dir.toString(), "Scanning directory...", listener);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (cancelled.get()) {
                        return FileVisitResult.TERMINATE;
                    }

                    filesScannedCounter.incrementAndGet();

                    try {
                        if (attrs.isSymbolicLink() && !options.isFollowLinks()) {
                            skippedFilesCounter.incrementAndGet();
                            return FileVisitResult.CONTINUE;
                        }

                        if (!options.isIncludeHidden() && isHiddenPath(file)) {
                            skippedFilesCounter.incrementAndGet();
                            return FileVisitResult.CONTINUE;
                        }

                        if (!attrs.isRegularFile()) {
                            skippedFilesCounter.incrementAndGet();
                            return FileVisitResult.CONTINUE;
                        }

                        long size = attrs.size();
                        if (!options.isSizeAllowed(size)) {
                            skippedFilesCounter.incrementAndGet();
                            return FileVisitResult.CONTINUE;
                        }

                        String fileName = file.getFileName() != null ? file.getFileName().toString() : "";
                        String extension = extractExtension(fileName);
                        if (!options.isExtensionAllowed(extension)) {
                            skippedFilesCounter.incrementAndGet();
                            return FileVisitResult.CONTINUE;
                        }

                        LocalDateTime created = toLocalDateTime(attrs.creationTime());
                        LocalDateTime modified = toLocalDateTime(attrs.lastModifiedTime());

                        FileItem item = FileItem.builder()
                                .originalPath(file)
                                .fileName(fileName)
                                .extension(extension)
                                .size(size)
                                .createdDate(created)
                                .modifiedDate(modified)
                                .build();

                        collectedFiles.add(item);
                        updateProgress(file.toString(), "Discovered: " + fileName, listener);

                    } catch (Exception e) {
                        skippedFilesCounter.incrementAndGet();
                        errors.add("Error inspecting file " + file + ": " + e.getMessage());
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    skippedFilesCounter.incrementAndGet();
                    if (exc != null) {
                        errors.add("Access denied or error reading " + file + ": " + exc.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (exc != null) {
                        errors.add("Error finishing directory " + dir + ": " + exc.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            Duration duration = Duration.between(startTime, Instant.now());

            if (cancelled.get()) {
                currentStatusString = "Cancelled";
                if (listener != null) {
                    listener.onCancelled();
                }
            }

            Statistics stats = Statistics.fromFiles(collectedFiles);

            ScanResult result = ScanResult.builder()
                    .files(collectedFiles)
                    .statistics(stats)
                    .duration(duration)
                    .filesScanned(filesScannedCounter.get())
                    .directoriesScanned(directoriesScannedCounter.get())
                    .skippedFiles(skippedFilesCounter.get())
                    .errors(errors)
                    .build();

            if (!cancelled.get() && listener != null) {
                listener.onComplete(result);
            }

            return result;

        } catch (Exception e) {
            currentStatusString = "Failed";
            if (listener != null) {
                listener.onError(e);
            }
            throw new RuntimeException("Scan operation failed for folder: " + folder, e);
        } finally {
            running.set(false);
        }
    }

    /**
     * Requests cancellation of an ongoing scan operation.
     */
    public void cancel() {
        if (running.get()) {
            cancelled.set(true);
        }
    }

    /**
     * Checks if a scan is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns a snapshot of current scan progress.
     *
     * @return ScanProgress instance
     */
    public ScanProgress getProgress() {
        return new ScanProgress(
                currentFileString,
                filesScannedCounter.get(),
                directoriesScannedCounter.get(),
                0.0,
                currentStatusString
        );
    }

    private void updateProgress(String path, String status, ScanListener listener) {
        this.currentFileString = path;
        this.currentStatusString = status;
        if (listener != null) {
            listener.onProgress(getProgress());
        }
    }

    private static boolean isHiddenPath(Path path) {
        try {
            if (Files.isHidden(path)) return true;
        } catch (IOException ignored) {
        }
        Path fn = path.getFileName();
        return fn != null && fn.toString().startsWith(".");
    }

    private static String extractExtension(String name) {
        if (name == null) return "";
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase().trim();
        }
        return "";
    }

    private static LocalDateTime toLocalDateTime(FileTime fileTime) {
        if (fileTime == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
    }
}
