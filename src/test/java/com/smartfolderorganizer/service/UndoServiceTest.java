package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UndoService Automated Unit Tests")
class UndoServiceTest {

    private final OrganizationService orgService = new OrganizationService();
    private final PreviewService previewService = new PreviewService();

    @Test
    @DisplayName("Should reverse successful file organization transaction (LIFO undo)")
    void shouldUndoSuccessfulTransaction(@TempDir Path sourceDir, @TempDir Path destDir) throws IOException {
        Path sourceFile = sourceDir.resolve("backup.zip");
        Files.writeString(sourceFile, "Zip file archive payload");

        FileItem fileItem = FileItem.builder()
                .originalPath(sourceFile)
                .size(Files.size(sourceFile))
                .category(Category.ARCHIVES)
                .modifiedDate(LocalDateTime.now())
                .selected(true)
                .build();

        PreviewResult preview = previewService.generatePreview(List.of(fileItem), destDir);
        var report = orgService.organize(preview);

        Transaction transaction = Transaction.builder()
                .operations(report.getMoveOperations())
                .completed(true)
                .build();

        TransactionHistory history = new TransactionHistory();
        history.addTransaction(transaction);

        UndoService undoService = new UndoService(history);
        UndoResult undoResult = undoService.undo(transaction);

        assertNotNull(undoResult);
        assertTrue(undoResult.isSuccessful());
        assertEquals(1, undoResult.getRestoredFiles());

        // File is moved back to original source path
        assertTrue(Files.exists(sourceFile));
        assertFalse(Files.exists(destDir.resolve("Archives").resolve("backup.zip")));
    }
}
