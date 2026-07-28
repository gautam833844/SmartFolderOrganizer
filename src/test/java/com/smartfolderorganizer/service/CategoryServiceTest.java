package com.smartfolderorganizer.service;

import com.smartfolderorganizer.model.Category;
import com.smartfolderorganizer.model.FileItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryService Automated Unit Tests")
class CategoryServiceTest {

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService();
    }

    @ParameterizedTest
    @CsvSource({
            "photo.jpg, IMAGES",
            "picture.PNG, IMAGES",
            "graphic.gif, IMAGES",
            "document.pdf, PDF",
            "notes.docx, DOCUMENTS",
            "presentation.pptx, DOCUMENTS",
            "movie.mp4, VIDEOS",
            "song.mp3, AUDIO",
            "archive.zip, ARCHIVES",
            "code.java, CODE",
            "script.py, CODE",
            "unknown.xyz, OTHERS"
    })
    @DisplayName("Should correctly detect category by file extension")
    void shouldDetectCategoryByExtension(String fileName, Category expectedCategory) {
        Category result = categoryService.detectCategory(Path.of(fileName));
        assertEquals(expectedCategory, result);
    }

    @Test
    @DisplayName("Should detect category from FileItem instance")
    void shouldDetectCategoryFromFileItem() {
        FileItem item = FileItem.builder()
                .originalPath(Path.of("C:/files/report.pdf"))
                .size(1024)
                .build();

        Category category = categoryService.detectCategory(item);
        assertEquals(Category.PDF, category);
    }

    @Test
    @DisplayName("Should handle null or empty path gracefully")
    void shouldHandleNullOrEmptyPath() {
        assertEquals(Category.OTHERS, categoryService.detectCategory((Path) null));
    }
}
