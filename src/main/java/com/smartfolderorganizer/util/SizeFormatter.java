package com.smartfolderorganizer.util;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting byte sizes into human-readable strings and parsing size representations.
 * <p>
 * Supports SI decimal units (KB, MB, GB, TB, PB) and IEC binary units (KiB, MiB, GiB, TiB, PiB).
 * </p>
 */
public final class SizeFormatter {

    private static final String[] DECIMAL_UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};
    private static final String[] BINARY_UNITS = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
    private static final Pattern SIZE_PATTERN = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([A-Za-z]+)?\\s*$");

    private SizeFormatter() {
        throw new UnsupportedOperationException("Utility class 'SizeFormatter' cannot be instantiated");
    }

    /**
     * Formats bytes into standard human-readable decimal units (e.g. 1.50 MB, 500 B).
     *
     * @param bytes quantity in bytes
     * @return formatted human readable size string
     */
    public static String format(long bytes) {
        return formatDecimal(bytes);
    }

    /**
     * Formats bytes into decimal units with specified decimal precision.
     *
     * @param bytes         quantity in bytes
     * @param decimalPlaces number of fractional decimal digits (0 to 6)
     * @return formatted size string
     */
    public static String format(long bytes, int decimalPlaces) {
        if (decimalPlaces < 0 || decimalPlaces > 6) {
            throw new IllegalArgumentException("decimalPlaces must be between 0 and 6");
        }
        if (bytes < 0) {
            return "-" + format(-bytes, decimalPlaces);
        }
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1000));
        exp = Math.min(exp, DECIMAL_UNITS.length - 1);
        double value = bytes / Math.pow(1000, exp);
        String formatSpec = "%." + decimalPlaces + "f %s";
        return String.format(Locale.US, formatSpec, value, DECIMAL_UNITS[exp]);
    }

    /**
     * Formats bytes using SI decimal units (1 KB = 1000 Bytes).
     *
     * @param bytes quantity in bytes
     * @return formatted size string
     */
    public static String formatDecimal(long bytes) {
        return format(bytes, 2);
    }

    /**
     * Formats bytes using IEC binary units (1 KiB = 1024 Bytes).
     *
     * @param bytes quantity in bytes
     * @return formatted binary size string (e.g. 1.43 MiB)
     */
    public static String formatBinary(long bytes) {
        if (bytes < 0) {
            return "-" + formatBinary(-bytes);
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        exp = Math.min(exp, BINARY_UNITS.length - 1);
        double value = bytes / Math.pow(1024, exp);
        return String.format(Locale.US, "%.2f %s", value, BINARY_UNITS[exp]);
    }

    /**
     * Parses a formatted size string (e.g. "1.5 MB", "500 KiB", "1024") into bytes.
     *
     * @param formattedSize input string to parse
     * @return equivalent byte count
     */
    public static long parseSizeToBytes(String formattedSize) {
        Objects.requireNonNull(formattedSize, "formattedSize must not be null");
        Matcher matcher = SIZE_PATTERN.matcher(formattedSize.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid size string format: " + formattedSize);
        }

        double val = Double.parseDouble(matcher.group(1));
        String unitStr = matcher.group(2);

        if (unitStr == null || unitStr.equalsIgnoreCase("B")) {
            return (long) val;
        }

        String unitLower = unitStr.toLowerCase(Locale.US);
        long multiplier;
        switch (unitLower) {
            case "k":
            case "kb":
                multiplier = 1000L;
                break;
            case "kib":
                multiplier = 1024L;
                break;
            case "m":
            case "mb":
                multiplier = 1000L * 1000L;
                break;
            case "mib":
                multiplier = 1024L * 1024L;
                break;
            case "g":
            case "gb":
                multiplier = 1000L * 1000L * 1000L;
                break;
            case "gib":
                multiplier = 1024L * 1024L * 1024L;
                break;
            case "t":
            case "tb":
                multiplier = 1000L * 1000L * 1000L * 1000L;
                break;
            case "tib":
                multiplier = 1024L * 1024L * 1024L * 1024L;
                break;
            default:
                throw new IllegalArgumentException("Unsupported size unit: " + unitStr);
        }

        return (long) (val * multiplier);
    }
}
