package io.auklet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    @Test void testIsNullOrEmpty() {
        assertTrue(Util.isNullOrEmpty(""));
        assertFalse(Util.isNullOrEmpty("1"));

        List<Integer> intList = new ArrayList<>();
        assertTrue(Util.isNullOrEmpty(intList));

        intList.add(1);
        assertFalse(Util.isNullOrEmpty(intList));
    }

    @Test void testOrElse() {
        assertEquals("0", Util.orElse("0", "1"));
        assertEquals("1", Util.orElse(null, "1"));
    }

    @Test void testOrElseNullEmpty() {
        assertEquals("0", Util.orElseNullEmpty("0", "1"));
        assertEquals("1", Util.orElseNullEmpty(null, "1"));
    }

    @Test void testRemoveTrailingSlash() {
        assertNull(Util.removeTrailingSlash(null));
        assertEquals("1", Util.removeTrailingSlash("1/"));
    }

    @Test void testAddLeadingSlash() {
        assertNull(Util.addLeadingSlash(null));
        assertEquals("/1", Util.addLeadingSlash("1"));
    }

    @Test void testGetCurrentStackTrace() {
        assertEquals(Util.getCurrentStackTrace()[0].toString(), "io.auklet.util.Util.getCurrentStackTrace(Util.java:153)");
    }

    @Test void testCloseQuietly() {
        final InputStream inputstream = getClass().getClassLoader().getResourceAsStream("response.json");
        Util.closeQuietly(inputstream);
        IOException e = assertThrows(
                IOException.class, new Executable() {
                    @Override
                    public void execute() throws IOException {
                        inputstream.available();
                    }
                });

        assertEquals(e.getMessage(), "Stream closed");
    }

    @Test void testGetMacAddressHash() {
        assertNotNull(Util.getMacAddressHash());
    }
}