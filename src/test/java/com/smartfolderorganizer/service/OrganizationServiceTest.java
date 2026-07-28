package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;
import com.smartfolderorganizer.model.OrganizationReport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrganizationService Automated Unit Tests")
class OrganizationServiceTest {

    private final OrganizationService organizationService = new OrganizationService();
    private final PreviewService previewService = new PreviewService();

    @Test
    @DisplayName("Should execute physical file move and create category directory")
    void shouldExecuteFileMoveAndCreateDirectories(@TempDir Path sourceDir, @TempDir Path destDir) throws IOException {
        Path sourceFile = sourceDir.resolve("document.pdf");
        Files.writeString(sourceFile, "Sample PDF Content");

        FileItem fileItem = FileItem.builder()
                .originalPath(sourceFile)
                .size(Files.size(sourceFile))
                .category(Category.PDF)
                .modifiedDate(LocalDateTime.now())
                .selected(true)
                .build();

        PreviewResult preview = previewService.generatePreview(List.of(fileItem), destDir);
        OrganizationReport report = organizationService.organize(preview, OrganizationOptions.defaultOptions());

        assertNotNull(report);
        assertEquals(1, report.getFilesOrganized().size());
        assertTrue(report.getFailedFiles().isEmpty());

        Path expectedTarget = destDir.resolve("PDFs").resolve("document.pdf");
        assertTrue(Files.exists(expectedTarget));
        assertFalse(Files.exists(sourceFile));
    }

    @Test
    @DisplayName("Should simulate move operations during dry-run mode without modifying disk")
    void shouldSimulateMoveDuringDryRun(@TempDir Path sourceDir, @TempDir Path destDir) throws IOException {
        Path sourceFile = sourceDir.resolve("photo.jpg");
        Files.writeString(sourceFile, "Fake Photo Data");

        FileItem fileItem = FileItem.builder()
                .originalPath(sourceFile)
                .size(Files.size(sourceFile))
                .category(Category.IMAGES)
                .modifiedDate(LocalDateTime.now())
                .selected(true)
                .build();

        PreviewResult preview = previewService.generatePreview(List.of(fileItem), destDir);
        OrganizationOptions options = OrganizationOptions.builder().dryRun(true).build();

        OrganizationReport report = organizationService.organize(preview, options);

        assertNotNull(report);
        assertEquals(1, report.getFilesOrganized().size());
        assertTrue(Files.exists(sourceFile)); // Original file remains untouched
    }
}
