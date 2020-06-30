package io.auklet.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataUsageConfigTest {
    private DataUsageConfig dataUsageConfig;

    @BeforeAll void setup() {
        dataUsageConfig = new DataUsageConfig(1, 2, 3, 4);
    }

    @Test void testGetEmissionPeriod() {
        assertEquals(1, dataUsageConfig.getEmissionPeriod());
    }

    @Test void testGetStorageLimit() {
        assertEquals(2, dataUsageConfig.getStorageLimit());
    }

    @Test void testGetCellularDataLimit() {
        assertEquals(3, dataUsageConfig.getCellularDataLimit());
    }

    @Test void testGetCellularPlanDate() {
        assertEquals(4, dataUsageConfig.getCellularPlanDate());
    }
}
