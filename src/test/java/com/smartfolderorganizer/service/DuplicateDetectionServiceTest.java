package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.DuplicateGroup;
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

@DisplayName("DuplicateDetectionService Automated Unit Tests")
class DuplicateDetectionServiceTest {

    private final DuplicateDetectionService duplicateService = new DuplicateDetectionService();

    @Test
    @DisplayName("Should detect identical duplicate files using cryptographic checksum hash")
    void shouldDetectDuplicateFilesByChecksum(@TempDir Path tempDir) throws IOException {
        Path file1 = tempDir.resolve("original.txt");
        Path file2 = tempDir.resolve("copy.txt");
        String content = "Identical duplicate file content 123456789";

        Files.writeString(file1, content);
        Files.writeString(file2, content);

        FileItem item1 = FileItem.builder().originalPath(file1).size(Files.size(file1)).modifiedDate(LocalDateTime.now()).category(Category.OTHERS).build();
        FileItem item2 = FileItem.builder().originalPath(file2).size(Files.size(file2)).modifiedDate(LocalDateTime.now()).category(Category.OTHERS).build();

        DuplicateDetectionOptions options = DuplicateDetectionOptions.builder()
                .compareBySize(true)
                .compareByChecksum(true)
                .hashAlgorithm("SHA-256")
                .minimumDuplicateSize(0)
                .build();

        DuplicateDetectionResult result = duplicateService.findDuplicates(List.of(item1, item2), options);

        assertNotNull(result);
        List<DuplicateGroup> groups = result.getDuplicateGroups();
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getFiles().size());
    }

    @Test
    @DisplayName("Should filter out files below minimum duplicate size threshold")
    void shouldFilterByMinimumDuplicateSize(@TempDir Path tempDir) throws IOException {
        Path smallFile1 = tempDir.resolve("small1.txt");
        Path smallFile2 = tempDir.resolve("small2.txt");

        Files.writeString(smallFile1, "tiny");
        Files.writeString(smallFile2, "tiny");

        FileItem item1 = FileItem.builder().originalPath(smallFile1).size(Files.size(smallFile1)).build();
        FileItem item2 = FileItem.builder().originalPath(smallFile2).size(Files.size(smallFile2)).build();

        DuplicateDetectionOptions options = DuplicateDetectionOptions.builder()
                .minimumDuplicateSize(1024) // 1 KB min
                .build();

        DuplicateDetectionResult result = duplicateService.findDuplicates(List.of(item1, item2), options);

        assertNotNull(result);
        assertTrue(result.getDuplicateGroups().isEmpty());
    }
}
