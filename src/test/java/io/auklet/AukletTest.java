package io.auklet;

import io.auklet.config.DeviceAuth;
import io.auklet.core.AukletApi;
import io.auklet.core.DataUsageMonitor;
import io.auklet.platform.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AukletTest extends TestingTools {
    private Auklet auklet;
    private TestLogger logger = TestLoggerFactory.getTestLogger(Auklet.class);
    private Runnable runnable;

    @BeforeAll void setup() throws AukletException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, RuntimeException {
        auklet = aukletConstructor(null);

        Config config = new Config().setAppId("0123456789101112")
                .setApiKey("123");

        runnable = new Runnable() {
            @Override
            public void run() {

            }
        };

        auklet.init(config);
    }

    @Test void testGetAppId() {
        assertEquals("0123456789101112", auklet.getAppId());
    }

    @Test void testGetBaseUrl() {
        assertEquals("https://api.auklet.io", auklet.getBaseUrl());
    }

    @Test void testGetConfigDir() {
        assertEquals(new File(".auklet").getAbsoluteFile(), auklet.getConfigDir());
    }

    @Test void testGetSerialPort() {
        assertNull(auklet.getSerialPort());
    }

    @Test void testGetMqttThreads() {
        assertEquals(true, auklet.getMqttThreads() > 1);
    }

    @Test void testGetMacHash() {
        assertNotNull(auklet.getMacHash());
    }

    @Test void testGetIpAddress() {
        assertNotNull(auklet.getIpAddress());
    }

    @Test void testGetApi() {
        assertThat(auklet.getApi(), instanceOf(AukletApi.class));
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

    @Test void testScheduleOneShotTask() throws AukletException {
        assertNotNull(auklet.scheduleOneShotTask(runnable, 1, TimeUnit.SECONDS));
    }

    @Test void testScheduleRepeatingTask() throws AukletException {
        assertNotNull(auklet.scheduleRepeatingTask(runnable, 1,1, TimeUnit.SECONDS));
    }
}