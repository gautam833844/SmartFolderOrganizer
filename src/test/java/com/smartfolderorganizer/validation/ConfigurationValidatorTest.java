package com.smartfolderorganizer.validation;

import com.smartfolderorganizer.exception.ValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigurationValidator Automated Unit Tests")
class ConfigurationValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"LIGHT", "DARK", "SYSTEM", "light", "Dark", "System"})
    @DisplayName("Should accept valid theme strings")
    void shouldAcceptValidThemes(String theme) {
        assertDoesNotThrow(() -> ConfigurationValidator.validateTheme(theme));
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid theme string")
    void shouldRejectInvalidTheme() {
        assertThrows(ValidationException.class, () -> ConfigurationValidator.validateTheme("NEON_BLUE"));
    }

    @Test
    @DisplayName("Should validate minimum window dimensions")
    void shouldValidateWindowSize() {
        assertDoesNotThrow(() -> ConfigurationValidator.validateWindowSize(1200.0, 800.0));
        assertThrows(ValidationException.class, () -> ConfigurationValidator.validateWindowSize(400.0, 300.0));
    }
}
