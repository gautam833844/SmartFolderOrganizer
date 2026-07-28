package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production-quality file system directory monitoring engine utilizing Java NIO {@link WatchService}.
 * <p>
 * Supports recursive directory registration, automatic monitoring of dynamically created subdirectories,
 * event debouncing, pause/resume, and real-time statistics.
 * </p>
 */
public class FolderWatchService {

    private final CategoryService categoryService;
    private final FolderWatchStatistics statistics = new FolderWatchStatistics();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private WatchService watchService;
    private ExecutorService executor;

    private final Map<WatchKey, Path> watchKeyPathMap = new ConcurrentHashMap<>();
    private final Map<Path, Long> debouncedEventsMap = new ConcurrentHashMap<>();

    private FolderWatchOptions currentOptions = FolderWatchOptions.defaultOptions();
    private FolderWatchListener currentListener;

    public FolderWatchService() {
        this(new CategoryService());
    }

    public FolderWatchService(CategoryService categoryService) {
        this.categoryService = Objects.requireNonNull(categoryService, "CategoryService must not be null");
    }

    public FolderWatchStatistics getStatistics() {
        return statistics;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isPaused() {
        return paused.get();
    }

    /**
     * Starts watching a directory with default options.
     *
     * @param folder directory path to monitor
     */
    public void start(Path folder) {
        start(folder, FolderWatchOptions.defaultOptions(), null);
    }

    /**
     * Starts watching a directory with options.
     *
     * @param folder  directory path to monitor
     * @param options configuration options
     */
    public void start(Path folder, FolderWatchOptions options) {
        start(folder, options, null);
    }

    /**
     * Starts directory monitoring with options and event listener callbacks.
     *
     * @param folder   directory path to monitor (non-null)
     * @param options  watch options (non-null)
     * @param listener event listener callback (nullable)
     */
    public synchronized void start(Path folder, FolderWatchOptions options, FolderWatchListener listener) {
        Objects.requireNonNull(folder, "folder must not be null");
        Objects.requireNonNull(options, "options must not be null");

        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Target folder must be an existing directory: " + folder);
        }

        if (running.get()) {
            stop();
        }

        this.currentOptions = options;
        this.currentListener = listener;
        this.statistics.reset();
        this.paused.set(false);

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.watchKeyPathMap.clear();
            this.debouncedEventsMap.clear();

            if (options.isRecursive()) {
                registerTree(folder);
            } else {
                registerSingleFolder(folder);
            }

            this.running.set(true);

            if (currentListener != null) {
                currentListener.onStart();
            }

            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FolderWatchService-Thread");
                t.setDaemon(true);
                return t;
            });

            this.executor.submit(this::watchLoop);

        } catch (IOException e) {
            this.running.set(false);
            if (currentListener != null) {
                currentListener.onError(e);
            }
            throw new RuntimeException("Failed to initialize WatchService for folder: " + folder, e);
        }
    }

    /**
     * Stops the directory watcher and closes WatchService resources.
     */
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        watchKeyPathMap.clear();
        debouncedEventsMap.clear();

        if (currentListener != null) {
            currentListener.onStop();
        }
    }

    /**
     * Pauses event processing. Events detected while paused will be ignored.
     */
    public void pause() {
        paused.set(true);
    }

    /**
     * Resumes event processing.
     */
    public void resume() {
        paused.set(false);
    }

    /**
     * Dynamically registers an additional directory to watch.
     *
     * @param directory directory path to register
     */
    public synchronized void registerFolder(Path directory) {
        if (running.get() && watchService != null && Files.isDirectory(directory)) {
            try {
                if (currentOptions.isRecursive()) {
                    registerTree(directory);
                } else {
                    registerSingleFolder(directory);
                }
            } catch (IOException e) {
                if (currentListener != null) {
                    currentListener.onError(e);
                }
            }
        }
    }

    private void watchLoop() {
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | java.nio.file.ClosedWatchServiceException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Path dir = watchKeyPathMap.get(key);
            if (dir == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path name = ev.context();
                Path child = dir.resolve(name);

                if (paused.get()) {
                    continue;
                }

                if (!currentOptions.isIncludeHidden() && isHiddenPath(child)) {
                    continue;
                }

                // Event debouncing
                if (isDebounced(child, currentOptions.getDebounceMillis())) {
                    continue;
                }

                processWatchEvent(kind, child);

                // Auto-register dynamically created subdirectories
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && currentOptions.isRecursive() && Files.isDirectory(child)) {
                    registerFolder(child);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                watchKeyPathMap.remove(key);
                if (watchKeyPathMap.isEmpty()) {
                    break;
                }
            }
        }

        running.set(false);
    }

    private void processWatchEvent(WatchEvent.Kind<?> kind, Path child) {
        boolean isDir = Files.isDirectory(child);
        Category category = !isDir ? categoryService.detectCategory(child) : null;

        FolderWatchEvent.EventType eventType;

        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            if (isDir) {
                eventType = FolderWatchEvent.EventType.DIRECTORY_CREATED;
                statistics.incrementDirectoriesCreated();
            } else {
                eventType = FolderWatchEvent.EventType.FILE_CREATED;
                statistics.incrementFilesCreated();
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            if (isDir) {
                eventType = FolderWatchEvent.EventType.DIRECTORY_DELETED;
                statistics.incrementDirectoriesDeleted();
            } else {
                eventType = FolderWatchEvent.EventType.FILE_DELETED;
                statistics.incrementFilesDeleted();
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            if (isDir) {
                return; // Ignore directory modify events
            }
            eventType = FolderWatchEvent.EventType.FILE_MODIFIED;
            statistics.incrementFilesModified();
        } else {
            return;
        }

        FolderWatchEvent watchEvent = new FolderWatchEvent(eventType, child, isDir, category);

        if (currentListener != null) {
            try {
                currentListener.onEvent(watchEvent);
            } catch (Exception e) {
                currentListener.onError(e);
            }
        }
    }

    private boolean isDebounced(Path path, long debounceMillis) {
        if (debounceMillis <= 0) return false;
        long now = System.currentTimeMillis();
        Long lastTime = debouncedEventsMap.put(path, now);
        return lastTime != null && (now - lastTime) < debounceMillis;
    }

    private void registerSingleFolder(Path dir) throws IOException {
        WatchKey key = dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
        );
        watchKeyPathMap.put(key, dir);
    }

    private void registerTree(Path startDir) throws IOException {
        Files.walkFileTree(startDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!currentOptions.isIncludeHidden() && isHiddenPath(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                registerSingleFolder(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isHiddenPath(Path path) {
        try {
            if (Files.isHidden(path)) return true;
        } catch (IOException ignored) {
        }
        Path fn = path.getFileName();
        return fn != null && fn.toString().startsWith(".");
    }
}
