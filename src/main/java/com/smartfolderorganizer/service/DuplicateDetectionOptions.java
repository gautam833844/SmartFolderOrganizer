package com.smartfolderorganizer.service;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable options configuring duplicate detection algorithms and comparison criteria.
 */
public final class DuplicateDetectionOptions {

    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA-1";
    public static final String SHA256 = "SHA-256";

    private static final Set<String> SUPPORTED_ALGORITHMS = Set.of(MD5, SHA1, SHA256);

    private final boolean compareByName;
    private final boolean compareByExtension;
    private final boolean compareBySize;
    private final boolean compareByChecksum;
    private final boolean compareByLastModified;
    private final long minimumDuplicateSize;
    private final String hashAlgorithm;

    private DuplicateDetectionOptions(Builder builder) {
        this.compareByName = builder.compareByName;
        this.compareByExtension = builder.compareByExtension;
        this.compareBySize = builder.compareBySize;
        this.compareByChecksum = builder.compareByChecksum;
        this.compareByLastModified = builder.compareByLastModified;

        if (builder.minimumDuplicateSize < 0) {
            throw new IllegalArgumentException("minimumDuplicateSize cannot be negative: " + builder.minimumDuplicateSize);
        }
        this.minimumDuplicateSize = builder.minimumDuplicateSize;

        String algo = builder.hashAlgorithm != null ? builder.hashAlgorithm.toUpperCase().trim() : SHA256;
        if (!SUPPORTED_ALGORITHMS.contains(algo)) {
            throw new IllegalArgumentException("Unsupported hash algorithm: " + builder.hashAlgorithm + ". Supported: " + SUPPORTED_ALGORITHMS);
        }
        this.hashAlgorithm = algo;
    }

    public boolean isCompareByName() {
        return compareByName;
    }

    public boolean isCompareByExtension() {
        return compareByExtension;
    }

    public boolean isCompareBySize() {
        return compareBySize;
    }

    public boolean isCompareByChecksum() {
        return compareByChecksum;
    }

    public boolean isCompareByLastModified() {
        return compareByLastModified;
    }

    public long getMinimumDuplicateSize() {
        return minimumDuplicateSize;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public static DuplicateDetectionOptions defaultOptions() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuplicateDetectionOptions options = (DuplicateDetectionOptions) o;
        return compareByName == options.compareByName &&
                compareByExtension == options.compareByExtension &&
                compareBySize == options.compareBySize &&
                compareByChecksum == options.compareByChecksum &&
                compareByLastModified == options.compareByLastModified &&
                minimumDuplicateSize == options.minimumDuplicateSize &&
                Objects.equals(hashAlgorithm, options.hashAlgorithm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(compareByName, compareByExtension, compareBySize, compareByChecksum, compareByLastModified, minimumDuplicateSize, hashAlgorithm);
    }

    @Override
    public String toString() {
        return "DuplicateDetectionOptions{" +
                "compareByName=" + compareByName +
                ", compareByExtension=" + compareByExtension +
                ", compareBySize=" + compareBySize +
                ", compareByChecksum=" + compareByChecksum +
                ", compareByLastModified=" + compareByLastModified +
                ", minimumDuplicateSize=" + minimumDuplicateSize +
                ", hashAlgorithm='" + hashAlgorithm + '\'' +
                '}';
    }

    /**
     * Builder for constructing immutable {@link DuplicateDetectionOptions}.
     */
    public static final class Builder {
        private boolean compareByName = false;
        private boolean compareByExtension = false;
        private boolean compareBySize = true;
        private boolean compareByChecksum = true;
        private boolean compareByLastModified = false;
        private long minimumDuplicateSize = 0L;
        private String hashAlgorithm = SHA256;

        public Builder compareByName(boolean compareByName) {
            this.compareByName = compareByName;
            return this;
        }

        public Builder compareByExtension(boolean compareByExtension) {
            this.compareByExtension = compareByExtension;
            return this;
        }

        public Builder compareBySize(boolean compareBySize) {
            this.compareBySize = compareBySize;
            return this;
        }

        public Builder compareByChecksum(boolean compareByChecksum) {
            this.compareByChecksum = compareByChecksum;
            return this;
        }

        public Builder compareByLastModified(boolean compareByLastModified) {
            this.compareByLastModified = compareByLastModified;
            return this;
        }

        public Builder minimumDuplicateSize(long minimumDuplicateSize) {
            this.minimumDuplicateSize = minimumDuplicateSize;
            return this;
        }

        public Builder hashAlgorithm(String hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
            return this;
        }

        public DuplicateDetectionOptions build() {
            return new DuplicateDetectionOptions(this);
        }
    }
}
