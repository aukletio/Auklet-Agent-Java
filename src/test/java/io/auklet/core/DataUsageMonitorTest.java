package io.auklet.core;

import io.auklet.TestingTools;
import io.auklet.config.DataUsageLimit;
import io.auklet.config.DataUsageTracker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;

import static junit.framework.TestCase.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataUsageMonitorTest extends TestingTools {
    private DataUsageMonitor dataUsageMonitor;
    private DataUsageLimit mockedDataUsageLimit;
    private DataUsageTracker mockedDataUsageTracker;

    @BeforeAll
    void setup() throws RuntimeException, NoSuchFieldException {
        dataUsageMonitor = new DataUsageMonitor();

        mockedDataUsageLimit = Mockito.spy(DataUsageLimit.class);
        mockedDataUsageTracker = Mockito.spy(DataUsageTracker.class);

        Mockito.doReturn(new DataUsageConfig(100, 100, 1000, 100)).when(mockedDataUsageLimit).getConfig();

        FieldSetter.setField(dataUsageMonitor, dataUsageMonitor.getClass().getDeclaredField("limit"), mockedDataUsageLimit);
        FieldSetter.setField(dataUsageMonitor, dataUsageMonitor.getClass().getDeclaredField("tracker"), mockedDataUsageTracker);
    }

    @Test void testGetUsageConfig() {
        assertNotNull(dataUsageMonitor.getUsageConfig());
    }

    @Test void testAddMoreData() {
        dataUsageMonitor.addMoreData(0);
        // TODO: Create test
    }

    @Test void testWillExceedLimit() {
        Mockito.doReturn((long) 100).when(mockedDataUsageTracker).getBytesSent();
        assertFalse(dataUsageMonitor.willExceedLimit(1000));
        assertFalse(dataUsageMonitor.willExceedLimit(0));
        assertTrue(dataUsageMonitor.willExceedLimit(100));
    }
}
