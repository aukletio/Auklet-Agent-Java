package io.auklet.platform.metrics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;
import io.auklet.Auklet;
import io.auklet.misc.OSMX;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


/** <p>Platform methods specific to Java SE (and variants).</p> */
public final class JavaMetrics extends AbstractMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaMetrics.class);
    public static Auklet agent;

    /**
     * <p>Constructor.</p>
     * 
     * @param aukletAgent
     */
    public JavaMetrics(@NonNull Auklet aukletAgent) {
        agent = aukletAgent;
    }

    /**
     * <p>Returns the CPU usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value. If running on Android 8 or higher, will always be zero.
     */
     public float getCpuUsage() {
        long processors = OSMX.BEAN.getAvailableProcessors();
        if (processors == 0) {
            Runnable runnableCPU = this.calculateCpuUsage();
            try {
                if (runnableCPU != null) agent.scheduleRepeatingTask(runnableCPU, 0L, 1L, TimeUnit.SECONDS);
            }
            catch (AukletException e) {
                LOGGER.warn("Couldn't calculate memory usage", e);
                return 0f;
            }
            return cpuUsage;
        } else {
            double loadAvg = OSMX.BEAN.getSystemLoadAverage();
            if (loadAvg >= 0) {
                cpuUsage = 100f * (float) (loadAvg / processors);
            } else {
                cpuUsage = 0f;
            }
            return cpuUsage;
        }
    }

    /**
     * <p>Returns the memory usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value.
     */
    @Override public double getMemoryUsage() {
        long freeMemSize = OSMX.BEAN.getFreePhysicalMemorySize();
        if (freeMemSize == -1) {
            try {
                Process p = Runtime.getRuntime().exec("free -b");
                p.waitFor();
                BufferedReader buff = new BufferedReader(new InputStreamReader(
                        p.getInputStream()));
                String line = buff.readLine();
                line = buff.readLine();
                String[] s = line.split("[ ]+", 7);
                synchronized (lock) {
                    long total = Long.parseLong(s[1]);
                    long free = Long.parseLong(s[3]);
                    return 100f * (1 - ((double) free / (double) total));
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.warn("Couldn't calculate memory usage", e);
                return 0d;
            }
        } else {
            double memUsage;
            long freeMem = OSMX.BEAN.getFreePhysicalMemorySize();
            long totalMem = OSMX.BEAN.getTotalPhysicalMemorySize();
            if (freeMem >= 0 && totalMem >= 0) {
                memUsage = 100 * (1 - ((double) freeMem / (double) totalMem));
            } else {
                memUsage = 0d;
            }
            return memUsage;
        }
    }
}
