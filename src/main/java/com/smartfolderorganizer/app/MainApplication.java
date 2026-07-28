package com.smartfolderorganizer.app;

import com.smartfolderorganizer.service.FolderWatcherManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;

/**
 * Primary JavaFX Application Entry Point.
 * Responsible for loading initial FXML views, stage positioning, and lifecycle management.
 */
public class MainApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);

    private static final String APP_TITLE = "Smart Folder Organizer";
    private static final double DEFAULT_WIDTH = 1400.0;
    private static final double DEFAULT_HEIGHT = 850.0;
    private static final double MIN_WIDTH = 1100.0;
    private static final double MIN_HEIGHT = 700.0;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Bootstrapping {} JavaFX Application...", APP_TITLE);

            URL fxmlLocation = getClass().getResource("/fxml/MainDashboard.fxml");
            if (fxmlLocation == null) {
                throw new IllegalStateException("Cannot locate /fxml/MainDashboard.fxml on classpath");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.setScene(scene);

            // Optional Icon Loading (Graceful fallback)
            InputStream iconStream = getClass().getResourceAsStream("/images/app-icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                logger.debug("Application icon '/images/app-icon.png' not found. Skipping icon assignment.");
            }

            primaryStage.centerOnScreen();
            primaryStage.show();

            logger.info("{} launched successfully.", APP_TITLE);

        } catch (Exception e) {
            logger.error("Critical failure during JavaFX application launch sequence", e);
            throw new RuntimeException("Failed to launch application UI", e);
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down {} cleanly...", APP_TITLE);
        FolderWatcherManager.getInstance().stopWatching();
        super.stop();
    }
}
