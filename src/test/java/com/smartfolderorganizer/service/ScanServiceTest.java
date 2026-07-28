package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScanService Automated Unit Tests")
class ScanServiceTest {

    private final ScanService scanService = new ScanService();

    @Test
    @DisplayName("Should scan directory recursively and populate FileItems")
    void shouldScanRecursiveDirectories(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("subfolder");
        Files.createDirectories(subDir);

        Files.writeString(tempDir.resolve("image.jpg"), "fake image data");
        Files.writeString(subDir.resolve("document.pdf"), "fake pdf content");

        ScanOptions options = ScanOptions.builder().recursive(true).includeHidden(false).build();
        ScanResult result = scanService.scan(tempDir, options);

        assertNotNull(result);
        List<FileItem> scannedFiles = result.getScannedFiles();
        assertEquals(2, scannedFiles.size());

        boolean hasImage = scannedFiles.stream().anyMatch(f -> f.getCategory() == Category.IMAGES);
        boolean hasPdf = scannedFiles.stream().anyMatch(f -> f.getCategory() == Category.PDF);

        assertTrue(hasImage);
        assertTrue(hasPdf);
    }

    @Test
    @DisplayName("Should handle empty folder scan cleanly")
    void shouldHandleEmptyFolderScan(@TempDir Path tempDir) {
        ScanResult result = scanService.scan(tempDir, ScanOptions.defaultOptions());

        assertNotNull(result);
        assertTrue(result.getScannedFiles().isEmpty());
        assertEquals(0, result.getTotalFiles());
    }

    @Test
    @DisplayName("Should throw RuntimeException on non-existent directory scan")
    void shouldThrowOnInvalidDirectory() {
        Path invalidPath = Path.of("Z:/NonExistentDirectory_12345");
        assertThrows(RuntimeException.class, () -> scanService.scan(invalidPath));
    }
}
