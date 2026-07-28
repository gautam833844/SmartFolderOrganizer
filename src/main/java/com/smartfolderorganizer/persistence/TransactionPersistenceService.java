package com.smartfolderorganizer.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.smartfolderorganizer.service.Transaction;
import com.smartfolderorganizer.service.TransactionHistory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe service for persisting, loading, clearing, and exporting {@link TransactionHistory} using Jackson JSON serialization.
 */
public class TransactionPersistenceService {

    private final Path historyFilePath;
    private final ObjectMapper mapper;

    public TransactionPersistenceService() {
        this(PersistenceConstants.getDefaultTransactionHistoryFilePath());
    }

    public TransactionPersistenceService(Path historyFilePath) {
        this.historyFilePath = Objects.requireNonNull(historyFilePath, "historyFilePath must not be null");
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Loads transaction history from the default JSON storage file.
     *
     * @param history target TransactionHistory store to populate
     * @return true if history loaded successfully
     */
    public synchronized boolean loadTransactionHistory(TransactionHistory history) {
        Objects.requireNonNull(history, "history store must not be null");
        if (!Files.exists(historyFilePath)) {
            return false;
        }

        try {
            List<Transaction> loaded = mapper.readValue(historyFilePath.toFile(), new TypeReference<List<Transaction>>() {});
            if (loaded != null) {
                history.clearHistory();
                for (Transaction tx : loaded) {
                    if (tx != null) {
                        history.addTransaction(tx);
                    }
                }
                return true;
            }
        } catch (IOException ignored) {
            // Graceful handling of corrupted history JSON
        }
        return false;
    }

    /**
     * Persists all transactions from a TransactionHistory store to the default JSON file.
     *
     * @param history transaction history store to save
     * @return true if save succeeded
     */
    public synchronized boolean saveTransactionHistory(TransactionHistory history) {
        Objects.requireNonNull(history, "history store must not be null");
        return exportHistory(history, historyFilePath);
    }

    /**
     * Exports transaction history to a custom target file path.
     *
     * @param history    history store
     * @param targetPath file path to write JSON
     * @return true if export succeeded
     */
    public synchronized boolean exportHistory(TransactionHistory history, Path targetPath) {
        Objects.requireNonNull(history, "history store must not be null");
        Objects.requireNonNull(targetPath, "targetPath must not be null");

        try {
            Path parent = targetPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            mapper.writeValue(targetPath.toFile(), history.getAllTransactions());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Deletes the default transaction history storage file from disk.
     *
     * @return true if file was deleted or did not exist
     */
    public synchronized boolean deleteHistoryFile() {
        try {
            if (Files.exists(historyFilePath)) {
                Files.delete(historyFilePath);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Path getHistoryFilePath() {
        return historyFilePath;
    }
}
