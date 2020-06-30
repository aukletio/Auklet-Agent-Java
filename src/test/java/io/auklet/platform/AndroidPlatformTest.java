package io.auklet.platform;

import android.os.Build;
import io.auklet.AukletException;
import io.auklet.TestingTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AndroidPlatformTest extends TestingTools {
    private AndroidPlatform androidPlatform;
    private final MessageBufferPacker msgpack = MessagePack.newDefaultBufferPacker();

    // Unable to test addSystemMetrics because I am unable to mock ActivityManager.MemoryInfo
    @BeforeAll void setup() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 27);
        androidPlatform = new AndroidPlatform(getTestContext());
        androidPlatform.start(aukletConstructor());
    }

    @Test void testNewAndroidPlatform() throws Exception {
        AukletException e = assertThrows(
                AukletException.class, new Executable() {
                    @Override
                    public void execute() throws Exception {
                        new AndroidPlatform(null);
                    }
                });
        assertEquals(e.getMessage(), "Android platform was given a non-Context object.");

        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 0);
        AukletException e2 = assertThrows(
                AukletException.class, new Executable() {
                    @Override
                    public void execute() throws Exception {
                        new AndroidPlatform(getTestContext());
                    }
                });
        assertEquals(e2.getMessage(), "Unsupported Android API level: 0");

        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 27);
    }

    @Test void testGetPossibleConfigDirs() {
        List dirs = androidPlatform.getPossibleConfigDirs("");
        assertEquals(dirs.get(0), "dir/.auklet");
    }
}
