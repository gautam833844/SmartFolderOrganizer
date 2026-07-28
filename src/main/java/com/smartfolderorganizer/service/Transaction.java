package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.MoveOperation;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Immutable transaction record representing an executed organization run that can be audited or undone.
 */
@JsonDeserialize(builder = Transaction.Builder.class)
public final class Transaction {

    private final UUID transactionId;
    private final LocalDateTime timestamp;
    private final List<MoveOperation> operations;
    private final boolean completed;
    private final String description;

    private Transaction(Builder builder) {
        this.transactionId = Objects.requireNonNull(builder.transactionId, "transactionId must not be null");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp must not be null");
        this.operations = List.copyOf(Objects.requireNonNull(builder.operations, "operations must not be null"));
        this.completed = builder.completed;
        this.description = builder.description != null ? builder.description : "";
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<MoveOperation> getOperations() {
        return operations;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets all successful MoveOperations within this transaction.
     *
     * @return unmodifiable list of successful MoveOperations
     */
    public List<MoveOperation> getSuccessfulOperations() {
        return operations.stream()
                .filter(MoveOperation::isSuccess)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets all failed MoveOperations within this transaction.
     *
     * @return unmodifiable list of failed MoveOperations
     */
    public List<MoveOperation> getFailedOperations() {
        return operations.stream()
                .filter(op -> !op.isSuccess())
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Checks if this transaction contains successful operations that can be undone.
     *
     * @return true if completed and has successful move operations
     */
    public boolean isUndoable() {
        return completed && !getSuccessfulOperations().isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", timestamp=" + timestamp +
                ", successfulOps=" + getSuccessfulOperations().size() +
                ", failedOps=" + getFailedOperations().size() +
                ", completed=" + completed +
                ", description='" + description + '\'' +
                '}';
    }

    /**
     * Builder for constructing immutable {@link Transaction}.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private UUID transactionId;
        private LocalDateTime timestamp;
        private List<MoveOperation> operations = Collections.emptyList();
        private boolean completed = true;
        private String description = "";

        public Builder() {
            this.transactionId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
        }

        public Builder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder operations(List<MoveOperation> operations) {
            this.operations = operations != null ? operations : Collections.emptyList();
            return this;
        }

        public Builder completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Transaction build() {
            if (this.transactionId == null) {
                this.transactionId = UUID.randomUUID();
            }
            if (this.timestamp == null) {
                this.timestamp = LocalDateTime.now();
            }
            return new Transaction(this);
        }
    }
}
