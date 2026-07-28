package com.smartfolderorganizer.app;

import javafx.application.Application;

/**
 * Main-Class Bootstrap Wrapper.
 * Exists to allow launching JavaFX applications from standard executable fat-JARs
 * without requiring JavaFX module parameters on the command line.
 */
public class AppLauncher {

    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }
}
