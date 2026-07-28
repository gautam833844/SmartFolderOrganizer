module com.smartfolderorganizer {
    // JavaFX Modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Logging & Diagnostics
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    // JSON Processing
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    // File I/O Utilities
    requires org.apache.commons.io;
    requires java.naming;

    // Active Package Exports
    exports com.smartfolderorganizer.app;
    exports com.smartfolderorganizer.controller;
    exports com.smartfolderorganizer.model;
    exports com.smartfolderorganizer.util;
    exports com.smartfolderorganizer.validation;
    exports com.smartfolderorganizer.exception;
    exports com.smartfolderorganizer.service;
    exports com.smartfolderorganizer.persistence;
    exports com.smartfolderorganizer.ui.navigation;

    // Reflective Opens for Frameworks (JavaFX FXML & Jackson)
    opens com.smartfolderorganizer.app to javafx.graphics, javafx.fxml;
    opens com.smartfolderorganizer.controller to javafx.fxml;
    opens com.smartfolderorganizer.ui.navigation to javafx.fxml;
    opens com.smartfolderorganizer.model to com.fasterxml.jackson.databind;
    opens com.smartfolderorganizer.service to com.fasterxml.jackson.databind;
    opens com.smartfolderorganizer.persistence to com.fasterxml.jackson.databind;
}
