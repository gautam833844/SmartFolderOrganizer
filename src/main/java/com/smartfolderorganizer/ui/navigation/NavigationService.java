package com.smartfolderorganizer.ui.navigation;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Service orchestrating view transitions, history stacks, lazy screen loading, and main window content replacement.
 * <p>
 * Replaces the center region of the application's root BorderPane without recreating stages or windows.
 * Supports Back and Forward navigation history tracking.
 * </p>
 */
public class NavigationService {

    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);

    private final ScreenLoader screenLoader = new ScreenLoader();
    private final Deque<Screen> backStack = new ArrayDeque<>();
    private final Deque<Screen> forwardStack = new ArrayDeque<>();

    private BorderPane rootBorderPane;
    private Node dashboardCenterNode;
    private Screen currentScreen = Screen.DASHBOARD;

    /**
     * Registers the root BorderPane and default center content node.
     *
     * @param rootBorderPane      Main application layout container.
     * @param dashboardCenterNode Original dashboard center content node.
     */
    public void setRootPane(BorderPane rootBorderPane, Node dashboardCenterNode) {
        this.rootBorderPane = rootBorderPane;
        this.dashboardCenterNode = dashboardCenterNode;
        this.currentScreen = Screen.DASHBOARD;
        logger.info("NavigationService bound to root BorderPane and Dashboard default content.");
    }

    /**
     * Navigates to a specified screen.
     *
     * @param screen Target screen.
     * @return true if navigation succeeded, false otherwise.
     */
    public boolean navigateTo(Screen screen) {
        if (screen == null) {
            logger.warn("Cannot navigate to null Screen.");
            return false;
        }

        if (screen == currentScreen) {
            logger.debug("Already on screen '{}'. Navigation skipped.", screen.name());
            return true;
        }

        Node targetNode = resolveScreenNode(screen);
        if (targetNode == null) {
            logger.error("Failed to resolve View Node for screen '{}'", screen.name());
            return false;
        }

        if (rootBorderPane != null) {
            if (currentScreen != null) {
                backStack.push(currentScreen);
            }
            forwardStack.clear();

            rootBorderPane.setCenter(targetNode);
            currentScreen = screen;
            logger.info("Successfully navigated to screen '{}'", screen.name());
            return true;
        } else {
            logger.error("Root BorderPane is not initialized in NavigationService.");
            return false;
        }
    }

    /**
     * Navigates back to the previous screen in history.
     *
     * @return true if back navigation succeeded, false if history is empty.
     */
    public boolean goBack() {
        if (backStack.isEmpty()) {
            logger.debug("Back navigation stack is empty.");
            return false;
        }

        Screen previousScreen = backStack.pop();
        Node targetNode = resolveScreenNode(previousScreen);
        if (targetNode != null && rootBorderPane != null) {
            forwardStack.push(currentScreen);
            rootBorderPane.setCenter(targetNode);
            currentScreen = previousScreen;
            logger.info("Navigated BACK to screen '{}'", previousScreen.name());
            return true;
        }
        return false;
    }

    /**
     * Navigates forward to the next screen in history.
     *
     * @return true if forward navigation succeeded, false if history is empty.
     */
    public boolean goForward() {
        if (forwardStack.isEmpty()) {
            logger.debug("Forward navigation stack is empty.");
            return false;
        }

        Screen nextScreen = forwardStack.pop();
        Node targetNode = resolveScreenNode(nextScreen);
        if (targetNode != null && rootBorderPane != null) {
            backStack.push(currentScreen);
            rootBorderPane.setCenter(targetNode);
            currentScreen = nextScreen;
            logger.info("Navigated FORWARD to screen '{}'", nextScreen.name());
            return true;
        }
        return false;
    }

    /**
     * Gets the currently active screen.
     * @return Current Screen enum.
     */
    public Screen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Checks whether Back navigation is available.
     * @return true if back stack is non-empty.
     */
    public boolean canGoBack() {
        return !backStack.isEmpty();
    }

    /**
     * Checks whether Forward navigation is available.
     * @return true if forward stack is non-empty.
     */
    public boolean canGoForward() {
        return !forwardStack.isEmpty();
    }

    private Node resolveScreenNode(Screen screen) {
        if (screen == Screen.DASHBOARD) {
            return dashboardCenterNode;
        }
        return screenLoader.loadScreen(screen);
    }
}
