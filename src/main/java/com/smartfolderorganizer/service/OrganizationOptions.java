package com.smartfolderorganizer.service;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Immutable configuration options governing file movement and organization execution.
 */
@JsonDeserialize(builder = OrganizationOptions.Builder.class)
public final class OrganizationOptions {

    private final boolean overwriteExisting;
    private final boolean createDirectories;
    private final boolean atomicMove;
    private final boolean verifyAfterMove;
    private final boolean deleteEmptyFolders;
    private final boolean continueOnError;
    private final boolean dryRun;

    private OrganizationOptions(Builder builder) {
        this.overwriteExisting = builder.overwriteExisting;
        this.createDirectories = builder.createDirectories;
        this.atomicMove = builder.atomicMove;
        this.verifyAfterMove = builder.verifyAfterMove;
        this.deleteEmptyFolders = builder.deleteEmptyFolders;
        this.continueOnError = builder.continueOnError;
        this.dryRun = builder.dryRun;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public boolean isCreateDirectories() {
        return createDirectories;
    }

    public boolean isAtomicMove() {
        return atomicMove;
    }

    public boolean isVerifyAfterMove() {
        return verifyAfterMove;
    }

    public boolean isDeleteEmptyFolders() {
        return deleteEmptyFolders;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public static OrganizationOptions defaultOptions() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationOptions options = (OrganizationOptions) o;
        return overwriteExisting == options.overwriteExisting &&
                createDirectories == options.createDirectories &&
                atomicMove == options.atomicMove &&
                verifyAfterMove == options.verifyAfterMove &&
                deleteEmptyFolders == options.deleteEmptyFolders &&
                continueOnError == options.continueOnError &&
                dryRun == options.dryRun;
    }

    @Override
    public int hashCode() {
        return Objects.hash(overwriteExisting, createDirectories, atomicMove, verifyAfterMove, deleteEmptyFolders, continueOnError, dryRun);
    }

    @Override
    public String toString() {
        return "OrganizationOptions{" +
                "overwriteExisting=" + overwriteExisting +
                ", createDirectories=" + createDirectories +
                ", atomicMove=" + atomicMove +
                ", verifyAfterMove=" + verifyAfterMove +
                ", deleteEmptyFolders=" + deleteEmptyFolders +
                ", continueOnError=" + continueOnError +
                ", dryRun=" + dryRun +
                '}';
    }

    /**
     * Builder for constructing immutable {@link OrganizationOptions}.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private boolean overwriteExisting = false;
        private boolean createDirectories = true;
        private boolean atomicMove = false;
        private boolean verifyAfterMove = true;
        private boolean deleteEmptyFolders = false;
        private boolean continueOnError = true;
        private boolean dryRun = false;

        public Builder overwriteExisting(boolean overwriteExisting) {
            this.overwriteExisting = overwriteExisting;
            return this;
        }

        public Builder createDirectories(boolean createDirectories) {
            this.createDirectories = createDirectories;
            return this;
        }

        public Builder atomicMove(boolean atomicMove) {
            this.atomicMove = atomicMove;
            return this;
        }

        public Builder verifyAfterMove(boolean verifyAfterMove) {
            this.verifyAfterMove = verifyAfterMove;
            return this;
        }

        public Builder deleteEmptyFolders(boolean deleteEmptyFolders) {
            this.deleteEmptyFolders = deleteEmptyFolders;
            return this;
        }

        public Builder continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public OrganizationOptions build() {
            return new OrganizationOptions(this);
        }
    }
}
