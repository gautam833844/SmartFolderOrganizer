package com.smartfolderorganizer.persistence;

import com.smartfolderorganizer.service.Transaction;
import com.smartfolderorganizer.service.TransactionHistory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransactionPersistenceService Automated Unit Tests")
class TransactionPersistenceServiceTest {

    @Test
    @DisplayName("Should save and reload transaction history to JSON file")
    void shouldSaveAndLoadTransactionHistory(@TempDir Path tempDir) {
        Path historyFile = tempDir.resolve("history.json");
        TransactionPersistenceService service = new TransactionPersistenceService(historyFile);

        TransactionHistory history = new TransactionHistory();
        Transaction tx = Transaction.builder().description("Test Session").completed(true).build();
        history.addTransaction(tx);

        boolean saved = service.saveTransactionHistory(history);
        assertTrue(saved);
        assertTrue(Files.exists(historyFile));

        TransactionHistory loadedHistory = new TransactionHistory();
        boolean loaded = service.loadTransactionHistory(loadedHistory);

        assertTrue(loaded);
        assertEquals(1, loadedHistory.getAllTransactions().size());
        assertEquals(tx.getTransactionId(), loadedHistory.getAllTransactions().get(0).getTransactionId());
    }

    @Test
    @DisplayName("Should gracefully recover from corrupt JSON history file")
    void shouldRecoverFromCorruptHistoryFile(@TempDir Path tempDir) throws IOException {
        Path historyFile = tempDir.resolve("corrupt_history.json");
        Files.writeString(historyFile, "{ INVALID_JSON_DATA... }");

        TransactionPersistenceService service = new TransactionPersistenceService(historyFile);
        TransactionHistory history = new TransactionHistory();

        boolean loaded = service.loadTransactionHistory(history);
        assertFalse(loaded);
        assertTrue(history.getAllTransactions().isEmpty());
    }
}
