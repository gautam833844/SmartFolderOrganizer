package com.smartfolderorganizer.ui;

import com.smartfolderorganizer.ui.navigation.Screen;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FXML Views Runtime Loading Tests")
class ScreenLoaderTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Toolkit already initialized
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Toolkit failed to initialize");
    }

    @ParameterizedTest
    @EnumSource(value = Screen.class, names = {"SCANNER", "PREVIEW", "ORGANIZATION_PROGRESS", "DUPLICATE_MANAGER", "HISTORY", "REPORTS", "SETTINGS"})
    @DisplayName("Should successfully load FXML layout and instantiate controller")
    void shouldLoadFxmlScreen(Screen screen) throws Throwable {
        assertNotNull(screen.getFxmlPath(), "FXML path must not be null for " + screen);
        URL fxmlUrl = getClass().getResource(screen.getFxmlPath());
        assertNotNull(fxmlUrl, "Resource not found on classpath: " + screen.getFxmlPath());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
        AtomicReference<Parent> rootRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent root = loader.load();
                rootRef.set(root);
                Object controller = loader.getController();
                assertNotNull(controller, "Controller must not be null for screen: " + screen);
            } catch (Throwable t) {
                exceptionRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout loading FXML for screen: " + screen);

        if (exceptionRef.get() != null) {
            throw exceptionRef.get();
        }

        assertNotNull(rootRef.get(), "Root parent node must not be null for screen: " + screen);
    }
}
