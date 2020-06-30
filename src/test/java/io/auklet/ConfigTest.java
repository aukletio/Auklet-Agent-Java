package io.auklet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigTest {
    private Config config;
    private class TestObject {}
    private TestObject testObject;

    @BeforeAll void setup() {
        config = new Config();
        testObject = new TestObject();
    }

    @Test void testSetAppId() {
        assertNotNull(config.setAppId(null));
        assertNotNull(config.setAppId("0"));
    }

    @Test void testSetApiKey() {
        assertNotNull(config.setApiKey(null));
        assertNotNull(config.setApiKey("0"));
    }

    @Test void testSetBaseUrl() {
        assertNotNull(config.setBaseUrl(null));
        assertNotNull(config.setBaseUrl("0"));
    }

    @Test void testSetConfigDir() {
        assertNotNull(config.setConfigDir(null));
        assertNotNull(config.setConfigDir("0"));
    }

    @Test void testSetAutoShutdown() {
        assertNotNull(config.setAutoShutdown(true));
    }

    @Test void testSetUncaughtExceptionHandler() {
        assertNotNull(config.setUncaughtExceptionHandler(true));
    }

    @Test void testSetSerialPort() {
        assertNotNull(config.setSerialPort(null));
        assertNotNull(config.setSerialPort("0"));
    }

    @Test void testSetAndroidContent() {
        assertNotNull(config.setAndroidContext(testObject));
    }

    @Test void testSetMqttThreads() {
        assertNotNull(config.setMqttThreads(null));
        assertNotNull(config.setMqttThreads(0));
        assertNotNull(config.setMqttThreads(1));
    }

    @Test void testGetAppId() {
        config.setAppId("0");
        assertEquals("0", config.getAppId());
    }

    @Test void testGetApiKey() {
        config.setApiKey("0");
        assertEquals("0", config.getApiKey());
    }

    @Test void testGetBaseUrl() {
        config.setBaseUrl("0");
        assertEquals("0", config.getBaseUrl());
    }

    @Test void testGetConfigDir() {
        config.setConfigDir("0");
        assertEquals("0", config.getConfigDir());
    }

    @Test void testGetAutoShutdown() {
        config.setAutoShutdown(true);
        assertEquals(true, config.getAutoShutdown());
    }

    @Test void testGetUncaughtExceptionHandler() {
        config.setUncaughtExceptionHandler(true);
        assertEquals(true, config.getUncaughtExceptionHandler());
    }

    @Test void testGetSerialPort() {
        config.setSerialPort("0");
        assertEquals("0", config.getSerialPort());
    }

    @Test void testGetAndroidContent() {
        config.setAndroidContext(testObject);
        assertNotNull(config.getAndroidContext());
    }

    @Test void getGetMqttThreads() {
        config.setMqttThreads(1);
        assertNotNull(config.getMqttThreads());
    }
}
