package com.smartfolderorganizer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Utility wrapper around SLF4J Logger creation and standard logging operations.
 * <p>
 * Ensures consistent log formatting, null-safe message parameters, and structured logging.
 * </p>
 */
public final class LoggerUtil {

    private LoggerUtil() {
        throw new UnsupportedOperationException("Utility class 'LoggerUtil' cannot be instantiated");
    }

    /**
     * Obtains an SLF4J Logger instance for the specified class.
     *
     * @param clazz target class (non-null)
     * @return Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Obtains an SLF4J Logger instance for a named component or category.
     *
     * @param name category or logger name (non-null)
     * @return Logger instance
     */
    public static Logger getLogger(String name) {
        Objects.requireNonNull(name, "logger name must not be null");
        return LoggerFactory.getLogger(name);
    }

    /**
     * Logs an INFO level message.
     *
     * @param logger target logger (non-null)
     * @param message format string or message
     * @param args arguments to format into message
     */
    public static void info(Logger logger, String message, Object... args) {
        Objects.requireNonNull(logger, "logger must not be null");
        if (logger.isInfoEnabled()) {
            logger.info(message, args);
        }
    }

    /**
     * Logs a WARN level message.
     *
     * @param logger target logger (non-null)
     * @param message format string or message
     * @param args arguments to format into message
     */
    public static void warn(Logger logger, String message, Object... args) {
        Objects.requireNonNull(logger, "logger must not be null");
        if (logger.isWarnEnabled()) {
            logger.warn(message, args);
        }
    }

    /**
     * Logs a DEBUG level message.
     *
     * @param logger target logger (non-null)
     * @param message format string or message
     * @param args arguments to format into message
     */
    public static void debug(Logger logger, String message, Object... args) {
        Objects.requireNonNull(logger, "logger must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug(message, args);
        }
    }

    /**
     * Logs an ERROR level message with exception cause.
     *
     * @param logger target logger (non-null)
     * @param message message description
     * @param throwable cause or exception (nullable)
     * @param args additional message arguments
     */
    public static void error(Logger logger, String message, Throwable throwable, Object... args) {
        Objects.requireNonNull(logger, "logger must not be null");
        if (logger.isErrorEnabled()) {
            if (throwable != null) {
                logger.error(message + " - Exception: " + throwable.getMessage(), throwable);
            } else {
                logger.error(message, args);
            }
        }
    }
}
