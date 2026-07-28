package com.smartfolderorganizer.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe transaction history store maintaining a bounded queue of executed organization runs.
 */
public class TransactionHistory {

    public static final int DEFAULT_MAX_CAPACITY = 50;

    private final int maxCapacity;
    private final List<Transaction> history = new ArrayList<>();

    public TransactionHistory() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public TransactionHistory(int maxCapacity) {
        if (maxCapacity < 1) {
            throw new IllegalArgumentException("maxCapacity must be at least 1: " + maxCapacity);
        }
        this.maxCapacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Adds a completed transaction to history. Automatically evicts the oldest transaction if max capacity is reached.
     *
     * @param transaction transaction to store
     */
    public synchronized void addTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        history.add(transaction);
        while (history.size() > maxCapacity) {
            history.remove(0);
        }
    }

    /**
     * Removes a transaction by ID.
     *
     * @param id transaction UUID
     * @return true if removed
     */
    public synchronized boolean removeTransaction(UUID id) {
        if (id == null) return false;
        return history.removeIf(tx -> tx.getTransactionId().equals(id));
    }

    /**
     * Finds a transaction by its UUID.
     *
     * @param id transaction UUID
     * @return Optional containing matching Transaction if present
     */
    public synchronized Optional<Transaction> getTransaction(UUID id) {
        if (id == null) return Optional.empty();
        return history.stream()
                .filter(tx -> tx.getTransactionId().equals(id))
                .findFirst();
    }

    /**
     * Gets the most recent transaction added to history.
     *
     * @return Optional containing latest Transaction or empty if history is empty
     */
    public synchronized Optional<Transaction> getLatestTransaction() {
        if (history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(history.get(history.size() - 1));
    }

    /**
     * Returns an unmodifiable list of all stored transactions.
     *
     * @return unmodifiable list of transactions
     */
    public synchronized List<Transaction> getAllTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    /**
     * Clears all transaction history records.
     */
    public synchronized void clearHistory() {
        history.clear();
    }

    /**
     * Gets current total stored transaction count.
     *
     * @return size
     */
    public synchronized int size() {
        return history.size();
    }
}
