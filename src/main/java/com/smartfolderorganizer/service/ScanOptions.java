package com.smartfolderorganizer.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Immutable configuration options governing the behavior of file system directory scanning.
 */
@JsonDeserialize(builder = ScanOptions.Builder.class)
public final class ScanOptions {

    private final boolean recursive;
    private final boolean includeHidden;
    private final boolean followLinks;
    private final int maximumDepth;
    private final Set<String> allowedExtensions;
    private final Set<String> excludedExtensions;
    private final long minimumSize;
    private final long maximumSize;

    private ScanOptions(Builder builder) {
        this.recursive = builder.recursive;
        this.includeHidden = builder.includeHidden;
        this.followLinks = builder.followLinks;

        if (builder.maximumDepth < 1) {
            throw new IllegalArgumentException("maximumDepth must be at least 1: " + builder.maximumDepth);
        }
        this.maximumDepth = builder.recursive ? builder.maximumDepth : 1;

        if (builder.minimumSize < 0) {
            throw new IllegalArgumentException("minimumSize cannot be negative: " + builder.minimumSize);
        }
        this.minimumSize = builder.minimumSize;

        if (builder.maximumSize < builder.minimumSize) {
            throw new IllegalArgumentException("maximumSize cannot be less than minimumSize");
        }
        this.maximumSize = builder.maximumSize;

        this.allowedExtensions = Collections.unmodifiableSet(cleanExtensions(builder.allowedExtensions));
        this.excludedExtensions = Collections.unmodifiableSet(cleanExtensions(builder.excludedExtensions));
    }

    public boolean isRecursive() {
        return recursive;
    }

    public boolean isIncludeHidden() {
        return includeHidden;
    }

    public boolean isFollowLinks() {
        return followLinks;
    }

    public int getMaximumDepth() {
        return maximumDepth;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public Set<String> getExcludedExtensions() {
        return excludedExtensions;
    }

    public long getMinimumSize() {
        return minimumSize;
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    /**
     * Checks whether a file extension passes extension filtering rules.
     *
     * @param extension file extension (without dot, lowercase)
     * @return true if allowed
     */
    public boolean isExtensionAllowed(String extension) {
        if (extension == null) {
            extension = "";
        }
        String cleanExt = extension.toLowerCase().trim().replaceFirst("^\\.", "");

        if (!excludedExtensions.isEmpty() && excludedExtensions.contains(cleanExt)) {
            return false;
        }
        if (!allowedExtensions.isEmpty()) {
            return allowedExtensions.contains(cleanExt);
        }
        return true;
    }

    /**
     * Checks whether a file size passes size filtering rules.
     *
     * @param size size in bytes
     * @return true if within min/max bounds
     */
    public boolean isSizeAllowed(long size) {
        return size >= minimumSize && size <= maximumSize;
    }

    public static ScanOptions defaultOptions() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static Set<String> cleanExtensions(Set<String> exts) {
        if (exts == null || exts.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> clean = new HashSet<>();
        for (String ext : exts) {
            if (ext != null && !ext.isBlank()) {
                clean.add(ext.toLowerCase().trim().replaceFirst("^\\.", ""));
            }
        }
        return clean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScanOptions options = (ScanOptions) o;
        return recursive == options.recursive &&
                includeHidden == options.includeHidden &&
                followLinks == options.followLinks &&
                maximumDepth == options.maximumDepth &&
                minimumSize == options.minimumSize &&
                maximumSize == options.maximumSize &&
                Objects.equals(allowedExtensions, options.allowedExtensions) &&
                Objects.equals(excludedExtensions, options.excludedExtensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recursive, includeHidden, followLinks, maximumDepth, allowedExtensions, excludedExtensions, minimumSize, maximumSize);
    }

    @Override
    public String toString() {
        return "ScanOptions{" +
                "recursive=" + recursive +
                ", includeHidden=" + includeHidden +
                ", followLinks=" + followLinks +
                ", maximumDepth=" + maximumDepth +
                ", allowedExtensions=" + allowedExtensions +
                ", excludedExtensions=" + excludedExtensions +
                ", minimumSize=" + minimumSize +
                ", maximumSize=" + maximumSize +
                '}';
    }

    /**
     * Builder for constructing immutable {@link ScanOptions}.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private boolean recursive = true;
        private boolean includeHidden = false;
        private boolean followLinks = false;
        private int maximumDepth = Integer.MAX_VALUE;
        private Set<String> allowedExtensions = Collections.emptySet();
        private Set<String> excludedExtensions = Collections.emptySet();
        private long minimumSize = 0L;
        private long maximumSize = Long.MAX_VALUE;

        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public Builder includeHidden(boolean includeHidden) {
            this.includeHidden = includeHidden;
            return this;
        }

        public Builder followLinks(boolean followLinks) {
            this.followLinks = followLinks;
            return this;
        }

        public Builder maximumDepth(int maximumDepth) {
            this.maximumDepth = maximumDepth;
            return this;
        }

        public Builder allowedExtensions(Set<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions != null ? allowedExtensions : Collections.emptySet();
            return this;
        }

        public Builder excludedExtensions(Set<String> excludedExtensions) {
            this.excludedExtensions = excludedExtensions != null ? excludedExtensions : Collections.emptySet();
            return this;
        }

        public Builder minimumSize(long minimumSize) {
            this.minimumSize = minimumSize;
            return this;
        }

        public Builder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public ScanOptions build() {
            return new ScanOptions(this);
        }
    }
}
