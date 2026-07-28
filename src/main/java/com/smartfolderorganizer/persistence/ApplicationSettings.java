package com.smartfolderorganizer.persistence;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import com.smartfolderorganizer.service.OrganizationOptions;
import com.smartfolderorganizer.service.ScanOptions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration entity storing application preferences, window geometry, default paths, and options.
 */
@JsonDeserialize(builder = ApplicationSettings.Builder.class)
public final class ApplicationSettings {

    private final String theme;
    private final double windowWidth;
    private final double windowHeight;
    private final double windowX;
    private final double windowY;
    private final String defaultScanFolder;
    private final String defaultDestinationFolder;
    private final ScanOptions scanOptions;
    private final OrganizationOptions organizationOptions;
    private final boolean autoSave;
    private final List<String> recentFolders;

    private ApplicationSettings(Builder builder) {
        this.theme = builder.theme != null ? builder.theme : "SYSTEM";
        this.windowWidth = builder.windowWidth > 0 ? builder.windowWidth : 1200.0;
        this.windowHeight = builder.windowHeight > 0 ? builder.windowHeight : 700.0;
        this.windowX = builder.windowX;
        this.windowY = builder.windowY;
        this.defaultScanFolder = builder.defaultScanFolder != null ? builder.defaultScanFolder : "";
        this.defaultDestinationFolder = builder.defaultDestinationFolder != null ? builder.defaultDestinationFolder : "";
        this.scanOptions = builder.scanOptions != null ? builder.scanOptions : ScanOptions.defaultOptions();
        this.organizationOptions = builder.organizationOptions != null ? builder.organizationOptions : OrganizationOptions.defaultOptions();
        this.autoSave = builder.autoSave;
        this.recentFolders = List.copyOf(Objects.requireNonNull(builder.recentFolders, "recentFolders must not be null"));
    }

    public String getTheme() {
        return theme;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public double getWindowX() {
        return windowX;
    }

    public double getWindowY() {
        return windowY;
    }

    public String getDefaultScanFolder() {
        return defaultScanFolder;
    }

    public String getDefaultDestinationFolder() {
        return defaultDestinationFolder;
    }

    public ScanOptions getScanOptions() {
        return scanOptions;
    }

    public OrganizationOptions getOrganizationOptions() {
        return organizationOptions;
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public List<String> getRecentFolders() {
        return recentFolders;
    }

    public static ApplicationSettings defaultSettings() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationSettings that = (ApplicationSettings) o;
        return Double.compare(that.windowWidth, windowWidth) == 0 &&
                Double.compare(that.windowHeight, windowHeight) == 0 &&
                Double.compare(that.windowX, windowX) == 0 &&
                Double.compare(that.windowY, windowY) == 0 &&
                autoSave == that.autoSave &&
                Objects.equals(theme, that.theme) &&
                Objects.equals(defaultScanFolder, that.defaultScanFolder) &&
                Objects.equals(defaultDestinationFolder, that.defaultDestinationFolder) &&
                Objects.equals(scanOptions, that.scanOptions) &&
                Objects.equals(organizationOptions, that.organizationOptions) &&
                Objects.equals(recentFolders, that.recentFolders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theme, windowWidth, windowHeight, windowX, windowY, defaultScanFolder, defaultDestinationFolder, scanOptions, organizationOptions, autoSave, recentFolders);
    }

    @Override
    public String toString() {
        return "ApplicationSettings{" +
                "theme='" + theme + '\'' +
                ", windowWidth=" + windowWidth +
                ", windowHeight=" + windowHeight +
                ", autoSave=" + autoSave +
                ", recentFoldersCount=" + recentFolders.size() +
                '}';
    }

    /**
     * Builder for constructing immutable {@link ApplicationSettings}.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String theme = "SYSTEM";
        private double windowWidth = 1200.0;
        private double windowHeight = 700.0;
        private double windowX = -1.0;
        private double windowY = -1.0;
        private String defaultScanFolder = "";
        private String defaultDestinationFolder = "";
        private ScanOptions scanOptions = ScanOptions.defaultOptions();
        private OrganizationOptions organizationOptions = OrganizationOptions.defaultOptions();
        private boolean autoSave = true;
        private List<String> recentFolders = Collections.emptyList();

        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public Builder windowWidth(double windowWidth) {
            this.windowWidth = windowWidth;
            return this;
        }

        public Builder windowHeight(double windowHeight) {
            this.windowHeight = windowHeight;
            return this;
        }

        public Builder windowX(double windowX) {
            this.windowX = windowX;
            return this;
        }

        public Builder windowY(double windowY) {
            this.windowY = windowY;
            return this;
        }

        public Builder defaultScanFolder(String defaultScanFolder) {
            this.defaultScanFolder = defaultScanFolder;
            return this;
        }

        public Builder defaultDestinationFolder(String defaultDestinationFolder) {
            this.defaultDestinationFolder = defaultDestinationFolder;
            return this;
        }

        public Builder scanOptions(ScanOptions scanOptions) {
            this.scanOptions = scanOptions;
            return this;
        }

        public Builder organizationOptions(OrganizationOptions organizationOptions) {
            this.organizationOptions = organizationOptions;
            return this;
        }

        public Builder autoSave(boolean autoSave) {
            this.autoSave = autoSave;
            return this;
        }

        public Builder recentFolders(List<String> recentFolders) {
            this.recentFolders = recentFolders != null ? recentFolders : Collections.emptyList();
            return this;
        }

        public ApplicationSettings build() {
            return new ApplicationSettings(this);
        }
    }
}
