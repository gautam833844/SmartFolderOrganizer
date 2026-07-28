package com.smartfolderorganizer.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the primary MainView layout.
 * Manages UI interactions for the application shell.
 */
public class MainViewController {

    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        logger.info("Initializing MainViewController shell view.");
        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }
    }
}
