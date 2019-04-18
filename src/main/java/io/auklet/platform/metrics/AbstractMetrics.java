package io.auklet.platform.metrics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public abstract class AbstractMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetrics.class);

    public final Object lock = new Object();
    @GuardedBy("lock") private long total = 0L;
    @GuardedBy("lock") private long totalBefore = 0L;
    @GuardedBy("lock") private long totalDiff = 0L;
    @GuardedBy("lock") private long work = 0L;
    @GuardedBy("lock") private long workBefore = 0L;
    @GuardedBy("lock") private long workDiff = 0L;
    @GuardedBy("lock") public float cpuUsage = 0;

    /**
     * <p>Returns a runnable task that periodically gets system CPU usage from the
     * Android device on which the agent is running. Android has no APIs available to
     * obtain this information, so we have to use a background thread to read the
     * {@code /proc/stat} file.</p>
     *
     * @return {@code null} iff running on Android 8 or higher, in which case no
     * background task will be executed.
     */
    @Nullable public Runnable calculateCpuUsage() {
        if (Build.VERSION.SDK_INT >= 26) return null;
        return new Runnable() {
            @Override
            public void run() {
                // Obtain current CPU load.
                String[] s = null;
                try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
                    String line = reader.readLine();
                    while ((line = reader.readLine()) != null) {
                        s = line.split("[ ]+", 9);
                        if (s[0] == "cpu") {
                            break;
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Unable to obtain CPU usage", e);
                    return;
                }
                synchronized (lock) {
                    if (s != null) {
                        work = Long.parseLong(s[1]) + Long.parseLong(s[2]) + Long.parseLong(s[3]);
                        total = work + Long.parseLong(s[4]) + Long.parseLong(s[5]) +
                                Long.parseLong(s[6]) + Long.parseLong(s[7]);
                        // Calculate CPU Percentage
                        if (totalBefore != 0) {
                            workDiff = work - workBefore;
                            totalDiff = total - totalBefore;
                            cpuUsage = workDiff * 100 / (float) totalDiff;
                        }
                        totalBefore = total;
                        workBefore = work;
                    }
                }
            }
        };
    }

    public abstract double getMemoryUsage();
    public abstract float getCpuUsage();
}