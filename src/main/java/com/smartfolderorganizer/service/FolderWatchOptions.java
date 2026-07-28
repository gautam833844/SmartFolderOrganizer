package com.smartfolderorganizer.service;

import java.util.Objects;

/**
 * Immutable configuration options governing directory file system watching behavior.
 */
public final class FolderWatchOptions {

    private final boolean recursive;
    private final boolean watchCreate;
    private final boolean watchDelete;
    private final boolean watchModify;
    private final boolean includeHidden;
    private final long debounceMillis;
    private final boolean autoOrganize;

    private FolderWatchOptions(Builder builder) {
        this.recursive = builder.recursive;
        this.watchCreate = builder.watchCreate;
        this.watchDelete = builder.watchDelete;
        this.watchModify = builder.watchModify;
        this.includeHidden = builder.includeHidden;
        if (builder.debounceMillis < 0) {
            throw new IllegalArgumentException("debounceMillis cannot be negative: " + builder.debounceMillis);
        }
        this.debounceMillis = builder.debounceMillis;
        this.autoOrganize = builder.autoOrganize;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public boolean isWatchCreate() {
        return watchCreate;
    }

    public boolean isWatchDelete() {
        return watchDelete;
    }

    public boolean isWatchModify() {
        return watchModify;
    }

    public boolean isIncludeHidden() {
        return includeHidden;
    }

    public long getDebounceMillis() {
        return debounceMillis;
    }

    public boolean isAutoOrganize() {
        return autoOrganize;
    }

    public static FolderWatchOptions defaultOptions() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FolderWatchOptions options = (FolderWatchOptions) o;
        return recursive == options.recursive &&
                watchCreate == options.watchCreate &&
                watchDelete == options.watchDelete &&
                watchModify == options.watchModify &&
                includeHidden == options.includeHidden &&
                debounceMillis == options.debounceMillis &&
                autoOrganize == options.autoOrganize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recursive, watchCreate, watchDelete, watchModify, includeHidden, debounceMillis, autoOrganize);
    }

    @Override
    public String toString() {
        return "FolderWatchOptions{" +
                "recursive=" + recursive +
                ", watchCreate=" + watchCreate +
                ", watchDelete=" + watchDelete +
                ", watchModify=" + watchModify +
                ", includeHidden=" + includeHidden +
                ", debounceMillis=" + debounceMillis +
                ", autoOrganize=" + autoOrganize +
                '}';
    }

    /**
     * Builder for constructing immutable {@link FolderWatchOptions}.
     */
    public static final class Builder {
        private boolean recursive = true;
        private boolean watchCreate = true;
        private boolean watchDelete = true;
        private boolean watchModify = true;
        private boolean includeHidden = false;
        private long debounceMillis = 500L;
        private boolean autoOrganize = false;

        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public Builder watchCreate(boolean watchCreate) {
            this.watchCreate = watchCreate;
            return this;
        }

        public Builder watchDelete(boolean watchDelete) {
            this.watchDelete = watchDelete;
            return this;
        }

        public Builder watchModify(boolean watchModify) {
            this.watchModify = watchModify;
            return this;
        }

        public Builder includeHidden(boolean includeHidden) {
            this.includeHidden = includeHidden;
            return this;
        }

        public Builder debounceMillis(long debounceMillis) {
            this.debounceMillis = debounceMillis;
            return this;
        }

        public Builder autoOrganize(boolean autoOrganize) {
            this.autoOrganize = autoOrganize;
            return this;
        }

        public FolderWatchOptions build() {
            return new FolderWatchOptions(this);
        }
    }
}
