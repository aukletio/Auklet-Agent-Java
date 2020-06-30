package io.auklet;

import io.auklet.config.DeviceAuth;
import io.auklet.core.DataUsageMonitor;
import io.auklet.platform.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AukletTest extends TestingTools {
    private Auklet auklet;
    private Config config;

    @BeforeAll void setup() throws AukletException, IOException, URISyntaxException {
        auklet = aukletConstructor();

        config = new Config().setAppId("0123456789101112")
                .setApiKey("123");

        Auklet.init(config);
    }

    @Test void testReflectionNotSupported() throws NoSuchMethodException {
        final Constructor<Auklet> constructor = Auklet.class.getDeclaredConstructor(config.getClass());
        constructor.setAccessible(true);
        InvocationTargetException e = assertThrows(
                InvocationTargetException.class, new Executable() {
                    @Override
                    public void execute() throws InstantiationException, IllegalAccessException, InvocationTargetException {
                        constructor.newInstance(config);
                    }
                });

        assertTrue(IllegalStateException.class.isInstance(e.getCause()));
        assertEquals(e.getCause().getMessage(), "Use Auklet.init() to initialize the agent.");
    }

    @Test void testSend() {
//        Auklet.send();
    }

    // Unable to test public methods in class since we removed reflection
    @Test void testGetAppId() {
        assertEquals("0123456789101112", auklet.getAppId());
    }

    @Test void testGetConfigDir() {
        assertEquals(new File(".auklet").getAbsoluteFile(), auklet.getConfigDir());
    }

    @Test void testGetSerialPort() {
        assertNull(auklet.getSerialPort());
    }

    @Test void testGetMqttThreads() {
        assertTrue(auklet.getMqttThreads() > 1);
    }

    @Test void testGetMacHash() {
        assertNotNull(auklet.getMacHash());
    }

    @Test void testGetIpAddress() {
        assertNotNull(auklet.getIpAddress());
    }

    @Test void testGetDeviceAuth() {
        assertThat(auklet.getDeviceAuth(), instanceOf(DeviceAuth.class));
    }

    @Test void testGetUsageMonitor() {
        assertThat(auklet.getUsageMonitor(), instanceOf(DataUsageMonitor.class));
    }

    @Test void testGetPlatform() {
        assertThat(auklet.getPlatform(), instanceOf(Platform.class));
    }
}