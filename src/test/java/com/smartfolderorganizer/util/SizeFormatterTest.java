package com.smartfolderorganizer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SizeFormatter Automated Unit Tests")
class SizeFormatterTest {

    @Test
    @DisplayName("Should correctly format byte sizes into readable units")
    void shouldFormatByteSizes() {
        assertEquals("0 B", SizeFormatter.format(0));
        assertEquals("500 B", SizeFormatter.format(500));
        assertEquals("1.00 KB", SizeFormatter.format(1000));
        assertEquals("1.02 KB", SizeFormatter.format(1024));
        assertEquals("1.00 KiB", SizeFormatter.formatBinary(1024));
    }
}
