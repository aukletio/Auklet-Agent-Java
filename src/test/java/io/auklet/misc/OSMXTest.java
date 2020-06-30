package io.auklet.misc;

import io.auklet.misc.OSMX;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OSMXTest {
    @Test void testGetName() {
        assertNotEquals("", OSMX.BEAN.getName());
    }

    @Test void testGetArch() {
        assertNotEquals("", OSMX.BEAN.getArch());
    }

    @Test void testGetVersion() {
        assertNotEquals("", OSMX.BEAN.getVersion());
    }

    @Test void testGetAvailableProcessors() {
        assertNotEquals(0, OSMX.BEAN.getAvailableProcessors());
    }

    @Test void testGetSystemLoadAverage() {
        assertNotEquals(0, OSMX.BEAN.getSystemLoadAverage());
    }

    @Test void testGetCommittedVirtualMemorySize() {
        assertNotEquals(0, OSMX.BEAN.getCommittedVirtualMemorySize());
    }

    @Test void testGetTotalSwapSpaceSize() {
        assertNotEquals(0, OSMX.BEAN.getTotalSwapSpaceSize());
    }

    @Test void testGetFreeSwapSpaceSize() {
        assertNotEquals(0, OSMX.BEAN.getFreeSwapSpaceSize());
    }

    @Test void testGetProcessCpuTime() {
        assertNotEquals(0, OSMX.BEAN.getProcessCpuTime());
    }

    @Test void testGetFreePhysicalMemorySize() {
        assertNotEquals(0, OSMX.BEAN.getFreePhysicalMemorySize());
    }

    @Test void testGetTotalPhysicalMemorySize() {
        assertNotEquals(0, OSMX.BEAN.getTotalPhysicalMemorySize());
    }

    @Test void testGetSystemCpuLoad() {
        assertNotEquals(-1, OSMX.BEAN.getSystemCpuLoad());
    }

    @Test void testGetProcessCpuLoad() {
        assertNotEquals(-1, OSMX.BEAN.getProcessCpuLoad());
    }

    @Test void testGetOpenFileDescriptorCount() {
        assertNotEquals(0, OSMX.BEAN.getOpenFileDescriptorCount());
    }

    @Test void testGetMaxFileDescriptorCount() {
        assertNotEquals(0, OSMX.BEAN.getMaxFileDescriptorCount());
    }
}