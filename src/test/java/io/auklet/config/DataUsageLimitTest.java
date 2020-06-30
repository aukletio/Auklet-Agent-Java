package io.auklet.config;

import io.auklet.AukletException;
import io.auklet.TestingTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataUsageLimitTest extends TestingTools {
    private DataUsageLimit dataUsageLimit;

    @BeforeAll void setup() throws AukletException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, RuntimeException {
        dataUsageLimit = Mockito.spy(DataUsageLimit.class);
        Mockito.doReturn(jsonDataLimits).when(dataUsageLimit).loadConfig();
        dataUsageLimit.start(aukletConstructor(null));
    }

    @Test void testGetName() {
        assertEquals("limits", dataUsageLimit.getName());
    }

    @Test void testGetConfig() {
        assertEquals(jsonDataLimits.at("config").at("emission_period").asLong() * 1000L, dataUsageLimit.getConfig().getEmissionPeriod());
    }

    @Test void testRefresh() throws AukletException {
        Mockito.doReturn(newJsonDataLimits).when(dataUsageLimit).fetchFromApi();
        dataUsageLimit.refresh();
        assertEquals(newJsonDataLimits.at("config").at("emission_period").asLong() * 1000L, dataUsageLimit.getConfig().getEmissionPeriod());
    }

    @Test void testReadFromDisk() throws IOException {
        Mockito.doReturn(jsonDataLimits.toString()).when(dataUsageLimit).getStringFromDisk();
        assertEquals(jsonDataLimits, dataUsageLimit.readFromDisk());

        Mockito.doReturn("").when(dataUsageLimit).getStringFromDisk();
        assertNull(dataUsageLimit.readFromDisk());

        Mockito.doReturn("{}").when(dataUsageLimit).getStringFromDisk();
        assertNull(dataUsageLimit.readFromDisk());

    }
}
