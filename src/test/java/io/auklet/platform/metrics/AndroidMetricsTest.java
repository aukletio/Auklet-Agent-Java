package io.auklet.platform.metrics;

import android.os.Build;
import io.auklet.AukletException;
import io.auklet.TestingTools;
import io.auklet.core.AukletDaemonExecutor;
import io.auklet.util.ThreadUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AndroidMetricsTest extends TestingTools {
    private AndroidMetrics androidMetrics;

    // Unable to test getMemoryUsage because I am unable to mock ActivityManager.MemoryInfo
    @BeforeAll void setup() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 22);
        androidMetrics = new AndroidMetrics(getTestContext());
    }

    @Test void testNewAndroidMetrics() throws Exception {
        AukletException e = assertThrows(
                AukletException.class, new Executable() {
                    @Override
                    public void execute() throws Exception {
                        new AndroidMetrics(null);
                    }
                });
        assertEquals(e.getMessage(), "Android context is null.");

        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 22);
    }

    @Test void testGetCPUUsage() {
        Runnable cpuUsage = androidMetrics.calculateCpuUsage();
        AukletDaemonExecutor DAEMON = new AukletDaemonExecutor(1, ThreadUtil.createDaemonThreadFactory("Auklet"));
        ScheduledFuture sc = DAEMON.scheduleAtFixedRate(cpuUsage, 0L, 1L, TimeUnit.MILLISECONDS);
        assertEquals(androidMetrics.getCpuUsage(), 0f);
        sc.cancel(true);
    }
}
