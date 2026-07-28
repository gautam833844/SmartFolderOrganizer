package com.smartfolderorganizer.ui.navigation;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton Coordinator for application-wide screen navigation.
 * <p>
 * Provides a clean API for UI controllers to initiate screen transitions, back/forward history traversal,
 * and current view tracking without tight coupling to layout containers.
 * </p>
 */
public class NavigationManager {

    private static final Logger logger = LoggerFactory.getLogger(NavigationManager.class);
    private static final NavigationManager INSTANCE = new NavigationManager();

    private final NavigationService navigationService = new NavigationService();
    private boolean initialized = false;

    private NavigationManager() {
        // Private constructor for singleton
    }

    /**
     * Gets the global NavigationManager singleton instance.
     * @return NavigationManager instance.
     */
    public static NavigationManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the NavigationManager with the primary application layout container.
     *
     * @param rootBorderPane      Primary application window BorderPane.
     * @param dashboardCenterNode Original center content for the main dashboard view.
     */
    public void initialize(BorderPane rootBorderPane, Node dashboardCenterNode) {
        if (rootBorderPane == null) {
            throw new IllegalArgumentException("Root BorderPane cannot be null.");
        }
        navigationService.setRootPane(rootBorderPane, dashboardCenterNode);
        initialized = true;
        logger.info("NavigationManager successfully initialized.");
    }

    /**
     * Navigates to the specified screen.
     *
     * @param screen Target screen enum.
     * @return true if transition was successful.
     */
    public boolean navigate(Screen screen) {
        checkInitialization();
        logger.info("Navigation requested -> Screen: '{}'", screen != null ? screen.name() : "null");
        return navigationService.navigateTo(screen);
    }

    /**
     * Navigates back to the previously visited screen.
     * @return true if back navigation succeeded.
     */
    public boolean goBack() {
        checkInitialization();
        return navigationService.goBack();
    }

    /**
     * Navigates forward to the next screen in history stack.
     * @return true if forward navigation succeeded.
     */
    public boolean goForward() {
        checkInitialization();
        return navigationService.goForward();
    }

    /**
     * Gets the currently active screen.
     * @return Active Screen enum.
     */
    public Screen getCurrentScreen() {
        checkInitialization();
        return navigationService.getCurrentScreen();
    }

    /**
     * Checks if back navigation is possible.
     * @return true if back history is non-empty.
     */
    public boolean canGoBack() {
        checkInitialization();
        return navigationService.canGoBack();
    }

    /**
     * Checks if forward navigation is possible.
     * @return true if forward history is non-empty.
     */
    public boolean canGoForward() {
        checkInitialization();
        return navigationService.canGoForward();
    }

    private void checkInitialization() {
        if (!initialized) {
            logger.warn("NavigationManager is being accessed before initialization!");
        }
    }
}
