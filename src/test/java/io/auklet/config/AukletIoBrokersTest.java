package io.auklet.config;

import io.auklet.TestingTools;

import io.auklet.AukletException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;

import java.io.IOException;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class AukletIoBrokersTest extends TestingTools {
    private AukletIoBrokers aukletIoBrokers;

    @BeforeAll void setup() {
        aukletIoBrokers = Mockito.spy(AukletIoBrokers.class);
    }

    @Test void testGetName() {
        assertEquals("brokers", aukletIoBrokers.getName());
    }

    @Test void testGetUrl() throws NoSuchFieldException {
        String value = "ssl://brokers:port";
        FieldSetter.setField(aukletIoBrokers, aukletIoBrokers.getClass().getDeclaredField("url"), value);
        assertEquals(value, aukletIoBrokers.getUrl());
    }

    @Test void testReadFromDisk() throws IOException {
        Mockito.doThrow(IOException.class).when(aukletIoBrokers).getStringFromDisk();
        assertNull(aukletIoBrokers.readFromDisk());

        Mockito.doReturn(jsonBrokers.toString()).when(aukletIoBrokers).getStringFromDisk();
        assertEquals(jsonBrokers, aukletIoBrokers.readFromDisk());

        Mockito.doReturn("").when(aukletIoBrokers).getStringFromDisk();
        assertNull(aukletIoBrokers.readFromDisk());
    }

    @Test void testWriteToDisk() throws AukletException {
        aukletIoBrokers.file = new File("tmp/TestAukletIoBroker");
        aukletIoBrokers.writeToDisk(jsonBrokers);
        assertEquals(true, aukletIoBrokers.file.length() > 0);
    }
}
