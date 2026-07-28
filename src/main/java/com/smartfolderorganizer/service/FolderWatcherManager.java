package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.persistence.ApplicationSettings;
import com.smartfolderorganizer.persistence.SettingsService;
import com.smartfolderorganizer.util.FileUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton orchestrator integrating {@link FolderWatchService} into the application lifecycle.
 * <p>
 * Monitors configured default directories, debounces rapid file events, updates application status properties,
 * and reuses existing {@link ScanService}, {@link PreviewService}, and {@link OrganizationService} instances to execute
 * automatic organization workflows.
 * </p>
 */
public class FolderWatcherManager {

    private static final Logger logger = LoggerFactory.getLogger(FolderWatcherManager.class);
    private static final FolderWatcherManager INSTANCE = new FolderWatcherManager();

    private final FolderWatchService folderWatchService = new FolderWatchService();
    private final SettingsService settingsService = new SettingsService();
    private final ScanService scanService = new ScanService();
    private final PreviewService previewService = new PreviewService();
    private final OrganizationService organizationService = new OrganizationService();

    private final StringProperty watcherStatusProperty = new SimpleStringProperty("Stopped");
    private final AtomicBoolean autoOrganizing = new AtomicBoolean(false);
    private final ExecutorService autoOrgExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AutoOrgPipeline-Worker");
        t.setDaemon(true);
        return t;
    });

    private FolderWatcherManager() {
    }

    public static FolderWatcherManager getInstance() {
        return INSTANCE;
    }

    public StringProperty watcherStatusProperty() {
        return watcherStatusProperty;
    }

    public String getWatcherStatus() {
        return watcherStatusProperty.get();
    }

    public FolderWatchService getFolderWatchService() {
        return folderWatchService;
    }

    /**
     * Initializes directory watching based on current application settings.
     */
    public synchronized void initializeFromSettings() {
        ApplicationSettings settings = settingsService.getSettings();
        String defaultFolderStr = settings.getDefaultScanFolder();

        if (defaultFolderStr == null || defaultFolderStr.isBlank()) {
            updateStatus("Idle (No default folder set)");
            logger.info("FolderWatcherManager remaining idle: No default scan folder configured.");
            return;
        }

        Path watchPath = Path.of(defaultFolderStr);
        if (!Files.exists(watchPath) || !Files.isDirectory(watchPath)) {
            updateStatus("Error (Default folder missing)");
            logger.warn("Target watch directory does not exist: {}", defaultFolderStr);
            return;
        }

        ScanOptions scanOpt = settings.getScanOptions();
        FolderWatchOptions watchOptions = FolderWatchOptions.builder()
                .recursive(scanOpt != null && scanOpt.isRecursive())
                .includeHidden(scanOpt != null && scanOpt.isIncludeHidden())
                .debounceMillis(500L)
                .autoOrganize(true)
                .build();

        startWatching(watchPath, watchOptions);
    }

    /**
     * Starts directory monitoring on a target folder with options.
     *
     * @param targetFolder Folder path to watch.
     * @param options      Watch options.
     */
    public synchronized void startWatching(Path targetFolder, FolderWatchOptions options) {
        Objects.requireNonNull(targetFolder, "targetFolder must not be null");
        Objects.requireNonNull(options, "options must not be null");

        try {
            folderWatchService.start(targetFolder, options, new FolderWatchListener() {
                @Override
                public void onStart() {
                    updateStatus("Running (" + targetFolder.getFileName() + ")");
                    logger.info("FolderWatchService started watching directory: {}", targetFolder);
                }

                @Override
                public void onEvent(FolderWatchEvent event) {
                    handleWatchEvent(event, targetFolder, options);
                }

                @Override
                public void onError(Exception ex) {
                    updateStatus("Error: " + ex.getMessage());
                    logger.error("FolderWatchService encountered an error", ex);
                }

                @Override
                public void onStop() {
                    updateStatus("Stopped");
                    logger.info("FolderWatchService stopped.");
                }
            });

        } catch (Exception ex) {
            updateStatus("Error (Start failed)");
            logger.error("Failed to start FolderWatchService", ex);
        }
    }

    /**
     * Stops the directory watcher engine.
     */
    public synchronized void stopWatching() {
        folderWatchService.stop();
        updateStatus("Stopped");
    }

    private void handleWatchEvent(FolderWatchEvent event, Path targetFolder, FolderWatchOptions options) {
        if (event == null) return;

        logger.info("Watcher Event Detected: {} on path: {}", event.getEventType(), event.getPath());

        if (event.getEventType() == FolderWatchEvent.EventType.FILE_CREATED || event.getEventType() == FolderWatchEvent.EventType.FILE_MODIFIED) {
            Path createdFile = event.getPath();

            if (options.isAutoOrganize()) {
                if (autoOrganizing.compareAndSet(false, true)) {
                    autoOrgExecutor.submit(() -> {
                        try {
                            updateStatus("Auto-Organizing " + createdFile.getFileName() + "...");
                            executeAutoOrganizationPipeline(createdFile, targetFolder);
                            updateStatus("Running (" + targetFolder.getFileName() + ")");
                        } catch (Exception ex) {
                            logger.error("Error during auto-organization pipeline", ex);
                            updateStatus("Running (Auto-Org Error)");
                        } finally {
                            autoOrganizing.set(false);
                        }
                    });
                }
            } else {
                updateStatus("New file detected: " + createdFile.getFileName() + " (Waiting)");
            }
        }
    }

    private void executeAutoOrganizationPipeline(Path newFile, Path targetFolder) {
        logger.info("Reusing existing ScanService, PreviewService, and OrganizationService for auto-organization of {}", newFile);

        // 1. Scan single file or parent folder
        ScanResult scanResult = scanService.scan(targetFolder, ScanOptions.defaultOptions());
        List<FileItem> scannedFiles = scanResult.getScannedFiles();

        if (scannedFiles.isEmpty()) return;

        // 2. Generate Organization Preview
        Path rootDest = targetFolder.resolve("Organized");
        PreviewResult previewResult = previewService.generatePreview(scannedFiles, rootDest);

        // 3. Execute Organization NIO Move
        if (!organizationService.isRunning()) {
            organizationService.organize(previewResult, OrganizationOptions.defaultOptions());
            logger.info("Auto-organization pipeline executed successfully for {}", newFile.getFileName());
        }
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> watcherStatusProperty.set(status));
    }
}
