package com.smartfolderorganizer.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Utility class for formatting timestamps, dates, and execution durations into ISO and human-readable strings.
 */
public final class DateTimeUtils {

    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter HUMAN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FILE_SAFE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class 'DateTimeUtils' cannot be instantiated");
    }

    /**
     * Gets current system date and time.
     *
     * @return current LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Gets current system date time formatted as ISO string.
     *
     * @return ISO-8601 formatted timestamp string
     */
    public static String nowIso() {
        return formatIso(now());
    }

    /**
     * Gets current system date time formatted in human readable form ("yyyy-MM-dd HH:mm:ss").
     *
     * @return formatted date time string
     */
    public static String nowFormatted() {
        return formatHumanReadable(now());
    }

    /**
     * Formats a LocalDateTime into standard ISO-8601 format.
     *
     * @param dateTime timestamp to format (non-null)
     * @return ISO string representation
     */
    public static String formatIso(LocalDateTime dateTime) {
        Objects.requireNonNull(dateTime, "dateTime must not be null");
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Formats a LocalDateTime into human-readable string ("yyyy-MM-dd HH:mm:ss").
     *
     * @param dateTime timestamp to format (non-null)
     * @return human readable timestamp string
     */
    public static String formatHumanReadable(LocalDateTime dateTime) {
        Objects.requireNonNull(dateTime, "dateTime must not be null");
        return dateTime.format(HUMAN_FORMATTER);
    }

    /**
     * Formats a LocalDateTime using a custom pattern.
     *
     * @param dateTime timestamp to format (non-null)
     * @param pattern  pattern string (non-null)
     * @return formatted timestamp string
     */
    public static String formatCustom(LocalDateTime dateTime, String pattern) {
        Objects.requireNonNull(dateTime, "dateTime must not be null");
        Objects.requireNonNull(pattern, "pattern must not be null");
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats a Duration into concise millisecond/second representation (e.g. "450 ms", "2.15 s").
     *
     * @param duration duration to format (non-null)
     * @return formatted duration string
     */
    public static String formatDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + " ms";
        }
        return String.format("%.2f s", millis / 1000.0);
    }

    /**
     * Formats a Duration into expanded human-readable form (e.g., "2m 15s", "1h 05m 30s", "450ms").
     *
     * @param duration duration to format (non-null)
     * @return human readable duration string
     */
    public static String formatDurationHuman(Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        long seconds = duration.getSeconds();
        long millis = duration.toMillisPart();

        if (seconds == 0) {
            return millis + "ms";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(secs).append("s");
        return sb.toString().trim();
    }
}
