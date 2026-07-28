package com.smartfolderorganizer.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain record representing an executed or attempted file move/copy/rename operation.
 * <p>
 * This class is completely immutable and maintains complete audit details of file transfers.
 * </p>
 */
public final class MoveOperation {

    private final UUID id;
    private final Path source;
    private final Path destination;
    private final LocalDateTime timestamp;
    private final boolean success;
    private final String message;

    private MoveOperation(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.source = Objects.requireNonNull(builder.source, "source path must not be null");
        this.destination = Objects.requireNonNull(builder.destination, "destination path must not be null");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp must not be null");
        this.success = builder.success;
        this.message = builder.message != null ? builder.message : "";
    }

    /**
     * Gets the operation unique ID.
     *
     * @return non-null UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the source path.
     *
     * @return non-null source Path
     */
    public Path getSource() {
        return source;
    }

    /**
     * Gets the target destination path.
     *
     * @return non-null destination Path
     */
    public Path getDestination() {
        return destination;
    }

    /**
     * Gets execution timestamp.
     *
     * @return non-null LocalDateTime
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if the move operation succeeded.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets operation result message or error trace summary.
     *
     * @return non-null result message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Creates a new MoveOperation updating success status and message.
     *
     * @param success success flag
     * @param message description or error message
     * @return new MoveOperation instance
     */
    public MoveOperation withSuccess(boolean success, String message) {
        return toBuilder().success(success).message(message).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .source(this.source)
                .destination(this.destination)
                .timestamp(this.timestamp)
                .success(this.success)
                .message(this.message);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveOperation that = (MoveOperation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MoveOperation{" +
                "id=" + id +
                ", source=" + source +
                ", destination=" + destination +
                ", timestamp=" + timestamp +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }

    /**
     * Builder for constructing immutable {@link MoveOperation} instances.
     */
    public static final class Builder {
        private UUID id;
        private Path source;
        private Path destination;
        private LocalDateTime timestamp;
        private boolean success;
        private String message = "";

        public Builder() {
            this.id = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder source(Path source) {
            this.source = source;
            return this;
        }

        public Builder destination(Path destination) {
            this.destination = destination;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public MoveOperation build() {
            if (this.id == null) {
                this.id = UUID.randomUUID();
            }
            if (this.timestamp == null) {
                this.timestamp = LocalDateTime.now();
            }
            return new MoveOperation(this);
        }
    }
}
