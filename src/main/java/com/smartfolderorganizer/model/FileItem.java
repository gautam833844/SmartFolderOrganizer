package com.smartfolderorganizer.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an individual file to be inspected, categorized, and organized.
 * <p>
 * This class is completely immutable. Modifying state (e.g. selection, destination) returns a new
 * instance via wither methods.
 * </p>
 */
public final class FileItem implements Comparable<FileItem> {

    private final UUID id;
    private final String fileName;
    private final String extension;
    private final Path originalPath;
    private final Path destinationPath;
    private final long size;
    private final LocalDateTime createdDate;
    private final LocalDateTime modifiedDate;
    private final Category category;
    private final boolean duplicate;
    private final boolean selected;

    private FileItem(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.originalPath = Objects.requireNonNull(builder.originalPath, "originalPath must not be null");
        this.fileName = Objects.requireNonNull(builder.fileName, "fileName must not be null");
        this.extension = Objects.requireNonNull(builder.extension, "extension must not be null");
        if (builder.size < 0) {
            throw new IllegalArgumentException("size cannot be negative: " + builder.size);
        }
        this.size = builder.size;
        this.createdDate = Objects.requireNonNull(builder.createdDate, "createdDate must not be null");
        this.modifiedDate = Objects.requireNonNull(builder.modifiedDate, "modifiedDate must not be null");
        this.category = Objects.requireNonNull(builder.category, "category must not be null");
        this.destinationPath = builder.destinationPath; // optional/nullable until computed
        this.duplicate = builder.duplicate;
        this.selected = builder.selected;
    }

    /**
     * Gets the unique identifier for this file item.
     *
     * @return non-null UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the file name including extension.
     *
     * @return non-null file name string
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the file extension without leading dot (lowercase).
     *
     * @return non-null extension string
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Gets the original absolute file system path.
     *
     * @return non-null Path
     */
    public Path getOriginalPath() {
        return originalPath;
    }

    /**
     * Gets the target destination path if set.
     *
     * @return destination Path or null if not yet determined
     */
    public Path getDestinationPath() {
        return destinationPath;
    }

    /**
     * Gets file size in bytes.
     *
     * @return non-negative file size
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets file creation timestamp.
     *
     * @return non-null LocalDateTime
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Gets file last modification timestamp.
     *
     * @return non-null LocalDateTime
     */
    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Gets assigned domain Category.
     *
     * @return non-null Category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Checks if this file has been flagged as a duplicate.
     *
     * @return true if duplicate
     */
    public boolean isDuplicate() {
        return duplicate;
    }

    /**
     * Checks if this file is selected for organization.
     *
     * @return true if selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Creates a new {@link FileItem} with the updated destination path.
     *
     * @param destinationPath new target destination path
     * @return new FileItem instance
     */
    public FileItem withDestinationPath(Path destinationPath) {
        return toBuilder().destinationPath(destinationPath).build();
    }

    /**
     * Creates a new {@link FileItem} with the updated duplicate flag.
     *
     * @param duplicate new duplicate flag state
     * @return new FileItem instance
     */
    public FileItem withDuplicate(boolean duplicate) {
        return toBuilder().duplicate(duplicate).build();
    }

    /**
     * Creates a new {@link FileItem} with the updated selected flag.
     *
     * @param selected new selection state
     * @return new FileItem instance
     */
    public FileItem withSelected(boolean selected) {
        return toBuilder().selected(selected).build();
    }

    /**
     * Creates a new {@link FileItem} with the updated Category.
     *
     * @param category new Category
     * @return new FileItem instance
     */
    public FileItem withCategory(Category category) {
        return toBuilder().category(category).build();
    }

    /**
     * Converts this FileItem into a Builder initialized with its current field values.
     *
     * @return new Builder instance
     */
    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .originalPath(this.originalPath)
                .destinationPath(this.destinationPath)
                .fileName(this.fileName)
                .extension(this.extension)
                .size(this.size)
                .createdDate(this.createdDate)
                .modifiedDate(this.modifiedDate)
                .category(this.category)
                .duplicate(this.duplicate)
                .selected(this.selected);
    }

    /**
     * Static factory method to start building a FileItem.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int compareTo(FileItem other) {
        Objects.requireNonNull(other, "Cannot compare FileItem with null");
        int nameCompare = this.fileName.compareToIgnoreCase(other.fileName);
        if (nameCompare != 0) {
            return nameCompare;
        }
        return this.originalPath.compareTo(other.originalPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileItem fileItem = (FileItem) o;
        return Objects.equals(id, fileItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FileItem{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", extension='" + extension + '\'' +
                ", size=" + size +
                ", category=" + category +
                ", duplicate=" + duplicate +
                ", selected=" + selected +
                ", originalPath=" + originalPath +
                ", destinationPath=" + destinationPath +
                '}';
    }

    /**
     * Builder for constructing immutable {@link FileItem} instances.
     */
    public static final class Builder {
        private UUID id;
        private String fileName;
        private String extension;
        private Path originalPath;
        private Path destinationPath;
        private long size;
        private LocalDateTime createdDate;
        private LocalDateTime modifiedDate;
        private Category category;
        private boolean duplicate = false;
        private boolean selected = true;

        public Builder() {
            this.id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            this.createdDate = now;
            this.modifiedDate = now;
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder originalPath(Path originalPath) {
            this.originalPath = originalPath;
            if (originalPath != null) {
                if (this.fileName == null && originalPath.getFileName() != null) {
                    this.fileName = originalPath.getFileName().toString();
                }
                if (this.extension == null && this.fileName != null) {
                    this.extension = extractExtension(this.fileName);
                }
            }
            return this;
        }

        public Builder destinationPath(Path destinationPath) {
            this.destinationPath = destinationPath;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            if (fileName != null && this.extension == null) {
                this.extension = extractExtension(fileName);
            }
            return this;
        }

        public Builder extension(String extension) {
            this.extension = extension != null ? extension.toLowerCase().trim().replaceFirst("^\\.", "") : "";
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder modifiedDate(LocalDateTime modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder duplicate(boolean duplicate) {
            this.duplicate = duplicate;
            return this;
        }

        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public FileItem build() {
            if (this.id == null) {
                this.id = UUID.randomUUID();
            }
            if (this.originalPath == null) {
                throw new IllegalStateException("originalPath is required to build FileItem");
            }
            if (this.fileName == null) {
                this.fileName = this.originalPath.getFileName() != null
                        ? this.originalPath.getFileName().toString()
                        : "unknown";
            }
            if (this.extension == null) {
                this.extension = extractExtension(this.fileName);
            }
            if (this.category == null) {
                this.category = Category.fromExtension(this.extension);
            }
            if (this.createdDate == null) {
                this.createdDate = LocalDateTime.now();
            }
            if (this.modifiedDate == null) {
                this.modifiedDate = LocalDateTime.now();
            }
            return new FileItem(this);
        }

        private static String extractExtension(String name) {
            if (name == null) return "";
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0 && lastDot < name.length() - 1) {
                return name.substring(lastDot + 1).toLowerCase().trim();
            }
            return "";
        }
    }
}
