package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PreviewService Automated Unit Tests")
class PreviewServiceTest {

    private final PreviewService previewService = new PreviewService();

    @Test
    @DisplayName("Should generate preview result with mapped destination paths")
    void shouldGeneratePreviewWithDestinationMapping(@TempDir Path targetDir) {
        Path src = Path.of("C:/downloads/vacation.jpg");
        FileItem item = FileItem.builder()
                .originalPath(src)
                .size(2048)
                .category(Category.IMAGES)
                .modifiedDate(LocalDateTime.now())
                .build();

        PreviewResult result = previewService.generatePreview(List.of(item), targetDir);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getPreview().getFiles().size());

        FileItem previewedItem = result.getPreview().getFiles().get(0);
        assertNotNull(previewedItem.getDestinationPath());
        assertTrue(previewedItem.getDestinationPath().toString().contains("Images"));
    }

    @Test
    @DisplayName("Should detect path conflicts during dry-run preview")
    void shouldDetectConflictsInPreview() {
        Path rootDest = Path.of("Z:/NonExistentRoot_12345");
        FileItem item = FileItem.builder()
                .originalPath(Path.of("C:/downloads/file.txt"))
                .size(100)
                .category(Category.OTHERS)
                .build();

        PreviewResult result = previewService.generatePreview(List.of(item), rootDest);

        assertNotNull(result);
        assertFalse(result.getConflicts().isEmpty());
    }
}
