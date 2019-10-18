package io.auklet.platform.metrics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.AukletException;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class handles retrieving memory and CPU usage for Android devices.</p>
 *
 * <p>CPU usage can only be retrieved on Android 7 or lower. When running on Android 8+,
 * this class will always report {@code 0} for CPU usage.</p>
 */
@ThreadSafe
public final class AndroidMetrics extends AbstractMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidMetrics.class);
    private final ActivityManager activityManager;

    private final Object lock = new Object();
    @GuardedBy("lock") private long total = 0L;
    @GuardedBy("lock") private long totalBefore = 0L;
    @GuardedBy("lock") private long totalDiff = 0L;
    @GuardedBy("lock") private long work = 0L;
    @GuardedBy("lock") private long workBefore = 0L;
    @GuardedBy("lock") private long workDiff = 0L;
    @GuardedBy("lock") private float cpuUsage = 0;

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
     * <p>Returns the memory usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value.
     */
    @Override public double getMemoryUsage() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        return memInfo.availMem / (double) memInfo.totalMem * 100.0;
    }

    /**
     * <p>Returns the CPU usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value. If running on Android 8 or higher, will always be zero.
     */
    @Override public float getCpuUsage() {
        synchronized (lock) {
            return cpuUsage;
        }
    }

}
