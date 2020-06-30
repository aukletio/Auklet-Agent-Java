package io.auklet.platform;

import io.auklet.AukletException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaPlatformTest {
    private JavaPlatform javaPlatform;
    private final MessageBufferPacker msgpack = MessagePack.newDefaultBufferPacker();

    @BeforeAll void setup() {
        javaPlatform = new JavaPlatform();
    }

    @Test void testGetPossibleConfigDirs() {
        assertNotNull(javaPlatform.getPossibleConfigDirs(null));
    }

    @Test void testAddSystemMetrics() throws IOException, AukletException {
        javaPlatform.addSystemMetrics(msgpack);
        assertNotNull(msgpack.toByteArray());
    }
}
