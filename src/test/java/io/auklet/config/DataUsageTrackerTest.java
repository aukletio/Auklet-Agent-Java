package io.auklet.config;

import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.TestingTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataUsageTrackerTest extends TestingTools {
    private DataUsageTracker dataUsageTracker;

    @BeforeAll void setup() throws AukletException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Auklet auklet = aukletConstructor(null);
        dataUsageTracker = new DataUsageTracker();

        dataUsageTracker.start(auklet);
    }

    @Test void testGetName() {
        assertEquals("usage", dataUsageTracker.getName());
    }

    @Test void testAddMoreData() {
        dataUsageTracker.addMoreData(0);
        assertEquals(0, dataUsageTracker.getBytesSent());

        dataUsageTracker.addMoreData(1L);
        assertEquals(1L, dataUsageTracker.getBytesSent());
    }

    @Test void testReset() {
        new File("aukletFiles/usage").delete();
        dataUsageTracker.addMoreData(1L);
        dataUsageTracker.reset();
        assertEquals(0L, dataUsageTracker.getBytesSent());
    }
}
