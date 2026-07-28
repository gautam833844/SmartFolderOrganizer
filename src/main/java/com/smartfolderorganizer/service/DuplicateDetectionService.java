package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.DuplicateGroup;
import com.smartfolderorganizer.model.FileItem;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * High-performance, multi-stage duplicate file detection engine.
 * <p>
 * Employs fast size/metadata pre-filtering before applying streaming cryptographic checksum hashes (MD5, SHA-1, SHA-256)
 * to avoid unnecessary disk I/O on unique files.
 * </p>
 */
public class DuplicateDetectionService {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Scans a list of FileItems for duplicates using default options.
     *
     * @param files target list of FileItems
     * @return DuplicateDetectionResult summary
     */
    public DuplicateDetectionResult findDuplicates(List<FileItem> files) {
        return findDuplicates(files, DuplicateDetectionOptions.defaultOptions(), null);
    }

    /**
     * Scans a list of FileItems for duplicates using specified options.
     *
     * @param files   target list of FileItems
     * @param options duplicate detection options
     * @return DuplicateDetectionResult summary
     */
    public DuplicateDetectionResult findDuplicates(List<FileItem> files, DuplicateDetectionOptions options) {
        return findDuplicates(files, options, null);
    }

    /**
     * Scans a list of FileItems for duplicates with options and real-time progress callbacks.
     *
     * @param files    target list of FileItems (non-null)
     * @param options  detection options (non-null)
     * @param listener progress listener (nullable)
     * @return DuplicateDetectionResult summary
     */
    public DuplicateDetectionResult findDuplicates(List<FileItem> files, DuplicateDetectionOptions options, DuplicateDetectionListener listener) {
        Objects.requireNonNull(files, "files list must not be null");
        Objects.requireNonNull(options, "options must not be null");

        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("DuplicateDetectionService is already executing a duplicate detection scan.");
        }

        cancelled.set(false);
        if (listener != null) {
            listener.onStart();
        }

        Instant startTime = Instant.now();
        List<String> errors = new CopyOnWriteArrayList<>();
        List<DuplicateGroup> duplicateGroups = new CopyOnWriteArrayList<>();
        AtomicLong processedCount = new AtomicLong(0);

        try {
            // Stage 1: Fast Filter - Filter by minimum size limit
            List<FileItem> candidates = files.stream()
                    .filter(Objects::nonNull)
                    .filter(f -> f.getSize() >= options.getMinimumDuplicateSize())
                    .collect(Collectors.toList());

            long totalCandidates = candidates.size();

            // Stage 2: Initial Structural Grouping (by Size, Name, Extension, LastModified)
            Map<String, List<FileItem>> preliminaryGroups = new HashMap<>();

            for (FileItem file : candidates) {
                if (cancelled.get()) break;

                StringBuilder keyBuilder = new StringBuilder();
                if (options.isCompareBySize()) {
                    keyBuilder.append("S:").append(file.getSize()).append(";");
                }
                if (options.isCompareByName()) {
                    keyBuilder.append("N:").append(file.getFileName().toLowerCase()).append(";");
                }
                if (options.isCompareByExtension()) {
                    keyBuilder.append("E:").append(file.getExtension().toLowerCase()).append(";");
                }
                if (options.isCompareByLastModified()) {
                    keyBuilder.append("M:").append(file.getModifiedDate()).append(";");
                }

                String groupKey = keyBuilder.length() > 0 ? keyBuilder.toString() : "GLOBAL";
                preliminaryGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(file);

                long curr = processedCount.incrementAndGet();
                if (listener != null && curr % 50 == 0) {
                    listener.onProgress(curr, totalCandidates);
                }
            }

            // Filter out unique items (groups with < 2 items)
            List<List<FileItem>> multiItemGroups = preliminaryGroups.values().stream()
                    .filter(list -> list.size() >= 2)
                    .collect(Collectors.toList());

            // Stage 3: Deep Verification (Checksum Hash Calculation)
            if (options.isCompareByChecksum() && !cancelled.get()) {
                Map<String, List<FileItem>> hashGroups = new ConcurrentHashMap<>();

                // Process candidate groups in parallel if dataset is large
                multiItemGroups.parallelStream().forEach(groupList -> {
                    if (cancelled.get()) return;

                    for (FileItem item : groupList) {
                        if (cancelled.get()) break;
                        try {
                            String hash = ChecksumCalculator.calculate(item.getOriginalPath(), options.getHashAlgorithm());
                            hashGroups.computeIfAbsent(hash, k -> new CopyOnWriteArrayList<>()).add(item);
                        } catch (Exception e) {
                            errors.add("Failed to compute hash for " + item.getOriginalPath() + ": " + e.getMessage());
                        }
                    }
                });

                hashGroups.forEach((hash, groupList) -> {
                    if (groupList.size() >= 2) {
                        DuplicateGroup group = DuplicateGroup.of(hash, new ArrayList<>(groupList));
                        duplicateGroups.add(group);
                        if (listener != null) {
                            listener.onDuplicateFound(group);
                        }
                    }
                });

            } else {
                // If checksum comparison disabled, structural groups >= 2 form duplicate groups
                for (List<FileItem> groupList : multiItemGroups) {
                    if (cancelled.get()) break;
                    String syntheticHash = "STRUCTURAL-" + groupList.hashCode();
                    DuplicateGroup group = DuplicateGroup.of(syntheticHash, groupList);
                    duplicateGroups.add(group);
                    if (listener != null) {
                        listener.onDuplicateFound(group);
                    }
                }
            }

            Duration duration = Duration.between(startTime, Instant.now());

            if (cancelled.get() && listener != null) {
                listener.onCancelled();
            }

            DuplicateDetectionResult result = DuplicateDetectionResult.builder()
                    .duplicateGroups(duplicateGroups)
                    .filesScanned(totalCandidates)
                    .duration(duration)
                    .errors(errors)
                    .build();

            if (!cancelled.get() && listener != null) {
                listener.onComplete(result);
            }

            return result;

        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
            throw new RuntimeException("Duplicate detection operation failed: " + e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }

    /**
     * Requests cancellation of an active duplicate detection scan.
     */
    public void cancel() {
        if (running.get()) {
            cancelled.set(true);
        }
    }

    /**
     * Checks if duplicate detection is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }
}
