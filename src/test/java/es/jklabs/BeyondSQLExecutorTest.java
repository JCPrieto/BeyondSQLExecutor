package es.jklabs;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BeyondSQLExecutorTest {

    @Test
    void mainRunsOnEdtInHeadlessMode() throws Exception {
        String previousHeadless = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        boolean executed = false;
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                return;
            }
            executed = true;

            Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
            CountDownLatch latch = new CountDownLatch(1);
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                failure.set(error);
                latch.countDown();
            });

            try {
                BeyondSQLExecutor.main(new String[0]);
                SwingUtilities.invokeAndWait(() -> {
                });
                latch.await(1, TimeUnit.SECONDS);
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(previousHandler);
            }
        } finally {
            if (previousHeadless == null) {
                System.clearProperty("java.awt.headless");
            } else {
                System.setProperty("java.awt.headless", previousHeadless);
            }
        }

        if (executed) {
            Throwable thrown = failure.get();
            assertNotNull(thrown, "Expected a HeadlessException when initializing the UI.");
            assertInstanceOf(HeadlessException.class, thrown, "Expected HeadlessException but got: " + thrown);
        }
    }
}
