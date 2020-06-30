package io.auklet.platform;

import io.auklet.AukletException;
import io.auklet.util.SysUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaPlatformTest {
    private JavaPlatform javaPlatform;
    private final MessageBufferPacker msgpack = MessagePack.newDefaultBufferPacker();

    @BeforeAll void setup() {
        javaPlatform = new JavaPlatform();
    }

    @Test void testGetPossibleConfigDirs() {
        String userDir = SysUtil.getSysProp("user.dir", false);
        String homeDir = SysUtil.getSysProp("user.home", false);
        String tmpDir = SysUtil.getSysProp("java.io.tmpdir", false);

        System.setProperty("user.dir", "userDir/");
        System.setProperty("user.home", "circleci");
        System.setProperty("java.io.tmpdir", "tmp");

        List<String> configDirs = javaPlatform.getPossibleConfigDirs("firstDir/");
        assertEquals(configDirs.get(0), "firstDir/.auklet");
        assertEquals(configDirs.get(1), "userDir/.auklet");
        assertEquals(configDirs.get(2), "circleci/.auklet");
        assertEquals(configDirs.get(3), "tmp/.auklet");

        // reset system vars back to what they originally were
        System.setProperty("user.dir", userDir);
        System.setProperty("user.home", homeDir);
        System.setProperty("java.io.tmpdir", tmpDir);
    }

    @Test void testAddSystemMetrics() throws IOException, AukletException {
        assertTrue(new String(msgpack.toByteArray()).isEmpty());
        javaPlatform.addSystemMetrics(msgpack);

        String msgpackString = new String(msgpack.toByteArray());
        assertFalse(msgpackString.isEmpty());
        assertTrue(msgpackString.contains("memoryUsage"));
        assertTrue(msgpackString.contains("cpuUsage"));
    }
}
