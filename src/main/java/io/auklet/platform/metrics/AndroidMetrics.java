package io.auklet.platform.metrics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * <p>This class handles retrieving memory and CPU usage for Android devices.</p>
 *
 * <p>CPU usage can only be retrieved on Android 7 or lower. When running on Android 8+,
 * this class will always report {@code 0} for CPU usage.</p>
 */
@ThreadSafe
public final class AndroidMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidMetrics.class);
    private final ActivityManager activityManager;

    private final Object lock = new Object();
    private long total = 0L;
    private long totalBefore = 0L;
    private long totalDiff = 0L;
    private long work = 0L;
    private long workBefore = 0L;
    private long workDiff = 0L;
    private float cpuUsage = 0;

    /**
     * <p>Constructor.</p>
     *
     * @param context the Android context.
     * @throws AukletException if context is {@code null}.
     */
    public AndroidMetrics(@NonNull Context context) throws AukletException {
        if (context == null) throw new AukletException("Android context is null.");
        if (Build.VERSION.SDK_INT >= 26) LOGGER.warn("Running on Android 8 or higher; system CPU stats will not be available.");
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

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
                String[] s;
                try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
                    s = reader.readLine().split("[ ]+", 9);
                } catch (IOException e) {
                    LOGGER.warn("Unable to obtain CPU usage", e);
                    return;
                }
                synchronized (lock) {
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
        };
    }

    /**
     * <p>Returns the memory usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value.
     */
    public double getMemoryUsage() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        return memInfo.availMem / (double) memInfo.totalMem * 100.0;
    }

    /**
     * <p>Returns the CPU usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value. If running on Android 8 or higher, will always be zero.
     */
    public float getCpuUsage() {
        synchronized (lock) {
            return cpuUsage;
        }
    }

}
