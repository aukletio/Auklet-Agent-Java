package io.auklet.misc;

import io.auklet.misc.PahoLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.ResourceBundle;

import static org.eclipse.paho.client.mqttv3.logging.Logger.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) class PahoLoggerTest {
    private PahoLogger pahoLogger = new PahoLogger();
    private TestLogger logger = TestLoggerFactory.getTestLogger("PahoLoggerTest");

    @BeforeAll void setup() {
        this.pahoLogger.initialise(null, "PahoLoggerTest", null);
    }

    @AfterEach void clearTestLoggerFactory() {
        TestLoggerFactory.clear();
    }

    @Test void testInitialise() {
        PahoLogger pahoLogger = new PahoLogger();
        ResourceBundle resourceBundle = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return null;
            }

            @Override
            public Enumeration<String> getKeys() {
                return null;
            }
        };
        try {
            pahoLogger.initialise(resourceBundle, null, "1");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: loggerID is null.", e.toString());
        }
    }

    @Test void testIsLoggable() {
        assertTrue(pahoLogger.isLoggable(SEVERE));
        assertTrue(pahoLogger.isLoggable(WARNING));
        assertTrue(pahoLogger.isLoggable(INFO));
        assertTrue(pahoLogger.isLoggable(CONFIG));
        assertTrue(pahoLogger.isLoggable(FINEST));
        assertFalse(pahoLogger.isLoggable(0));
    }

    @Test void testSevere() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLevels("TestSevere", "severe");
    }

    @Test void testWarning() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLevels("TestWarning", "warning");
    }

    @Test void testInfo() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLevels("TestInfo", "info");
    }

    @Test void testConfig() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLevels("TestConfig", "config");
    }

    @Test void testFine() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLevels("TestFine", "fine");
    }

    @Test void testFiner() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLevels("TestFiner", "finer");
    }

    @Test void testFinest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLevels("TestFinest", "finest");
    }

    @Test void testLog() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLogTrace("TestLog", "log");
    }

    @Test void testTrace() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        testPahoLogTrace("TestTrace", "trace");
    }

    private void testPahoLevels(String message, String level) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Method testMethod3Args = (pahoLogger.getClass().getDeclaredMethod(level, String.class, String.class, String.class));
        testMethod3Args.invoke(pahoLogger, "SourceClass", "SourceMethod", message);
        assertTrue(logger.getLoggingEvents().toString().contains(message));

        Method testMethod4Args = (pahoLogger.getClass().getDeclaredMethod(level, String.class, String.class, String.class, Object[].class));
        testMethod4Args.invoke(pahoLogger, "SourceClass", "SourceMethod", message, null);
        assertTrue(logger.getLoggingEvents().toString().contains(message));

        Method testMethod5Args = (pahoLogger.getClass().getDeclaredMethod(level, String.class, String.class, String.class, Object[].class, Throwable.class));
        testMethod5Args.invoke(pahoLogger, "SourceClass", "SourceMethod", message, null, null);
        assertTrue(logger.getLoggingEvents().toString().contains(message));
    }

    private void testPahoLogTrace(String message, String type) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method testMethod3Args = (pahoLogger.getClass().getDeclaredMethod(type, int.class, String.class, String.class, String.class, Object[].class, Throwable.class));
        testMethod3Args.invoke(pahoLogger, 0, "SourceClass", "SourceMethod", message, null, null);
        assertFalse(logger.getLoggingEvents().asList().toString().contains(message));
    }
}
