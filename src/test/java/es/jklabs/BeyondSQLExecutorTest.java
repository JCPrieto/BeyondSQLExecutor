package es.jklabs;

import org.junit.jupiter.api.Assumptions;
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
        Assumptions.assumeTrue(GraphicsEnvironment.isHeadless(), "Headless mode required for this test.");

        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        AtomicReference<Throwable> failure = new AtomicReference<>();
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
            if (previousHeadless == null) {
                System.clearProperty("java.awt.headless");
            } else {
                System.setProperty("java.awt.headless", previousHeadless);
            }
        }

        Throwable thrown = failure.get();
        assertNotNull(thrown, "Expected a HeadlessException when initializing the UI.");
        assertInstanceOf(HeadlessException.class, thrown, "Expected HeadlessException but got: " + thrown);
    }
}
