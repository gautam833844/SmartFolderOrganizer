package com.smartfolderorganizer.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Robust FXML View Loader with lazy loading, caching, and graceful error handling.
 * <p>
 * Ensures views are loaded from classpath FXML files lazily upon first request, cached in memory,
 * and errors are intercepted with non-crashing alert dialogs.
 * </p>
 */
public class ScreenLoader {

    private static final Logger logger = LoggerFactory.getLogger(ScreenLoader.class);

    private final Map<Screen, Parent> viewCache = new ConcurrentHashMap<>();
    private final Map<Screen, Object> controllerCache = new ConcurrentHashMap<>();

    /**
     * Retrieves or lazily loads the Parent root node for a given screen.
     *
     * @param screen Target screen enum.
     * @return Loaded Parent node, or null if loading failed.
     */
    public Parent loadScreen(Screen screen) {
        if (screen == null) {
            logger.warn("Attempted to load null Screen reference.");
            return null;
        }

        if (viewCache.containsKey(screen)) {
            logger.debug("Retrieving screen '{}' from view cache.", screen.name());
            return viewCache.get(screen);
        }

        if (screen.getFxmlPath() == null) {
            logger.debug("Screen '{}' does not have an external FXML path assigned.", screen.name());
            return null;
        }

        try {
            logger.info("Lazily loading FXML resource for screen '{}' from '{}'", screen.name(), screen.getFxmlPath());
            URL location = getClass().getResource(screen.getFxmlPath());
            if (location == null) {
                throw new IllegalStateException("FXML resource file not found on classpath: " + screen.getFxmlPath());
            }

            FXMLLoader loader = new FXMLLoader(location);
            Parent root = loader.load();
            Object controller = loader.getController();

            viewCache.put(screen, root);
            if (controller != null) {
                controllerCache.put(screen, controller);
            }

            logger.info("Successfully loaded and cached screen '{}'", screen.name());
            return root;

        } catch (Exception e) {
            logger.error("Failed to load FXML layout for screen '{}'", screen.name(), e);
            showErrorAlert(screen.getTitle(), e.getMessage());
            return null;
        }
    }

    /**
     * Gets the cached controller instance for a screen, if available.
     *
     * @param screen Target screen enum.
     * @return Controller object or null.
     */
    public Object getController(Screen screen) {
        return controllerCache.get(screen);
    }

    /**
     * Clears all cached screen views and controllers.
     */
    public void clearCache() {
        viewCache.clear();
        controllerCache.clear();
        logger.info("ScreenLoader cache cleared.");
    }

    private void showErrorAlert(String title, String details) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to Load Screen: " + title);
            alert.setContentText("An unexpected error occurred while loading the view layout.\n\nDetails: " + details);
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error("Could not display JavaFX Error Alert dialog", ex);
        }
    }
}
