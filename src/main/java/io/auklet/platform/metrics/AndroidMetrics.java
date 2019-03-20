package io.auklet.platform.metrics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * <p>This class handles retrieving the memory and cpu usage for Android devices.</p>
 *
 * <p>cpu usage can only be retrieved for devices running on Android 7 or older. If your device is running on
 * something newer than Android 7, then the cpu usage will return 0.</p>
 */
public final class AndroidMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidMetrics.class);
    private ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
    private ActivityManager manager;

    private long total;
    private long totalBefore;
    private long totalDiff;
    private long work;
    private long workBefore;
    private long workDiff;
    private float cpuUsage = 0;

    /**
     * <p>Constructor that creates the Activity Manager in order to be able to calculate the memory used in android.
     * Also initializes global variables used for calculating cpu usage</p>
     *
     * @param context the Android context.
     */
    public AndroidMetrics(Context context) {
        LOGGER.debug("We are unable to attain cpu data on devices running Android 8+");
        manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        manager.getMemoryInfo(memInfo);
        total = totalBefore = totalDiff = work = workBefore = workDiff = 0L;
    }

    @Nullable public Runnable calculateCpuUsage() {
        // On Android 8+, Google has restricted access to the proc files
        if (Build.VERSION.SDK_INT < 26) {
            return new Runnable() {
                @Override
                public void run() {
                    try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
                        // Obtain current CPU Load
                        String[] s = reader.readLine().split("[ ]+", 9);
                        work = Long.parseLong(s[1]) + Long.parseLong(s[2]) + Long.parseLong(s[3]);
                        total = work + Long.parseLong(s[4]) + Long.parseLong(s[5]) +
                                Long.parseLong(s[6]) + Long.parseLong(s[7]);
                    } catch (IOException e) {
                        LOGGER.error("Unable to obtain cpu usage", e);
                        return;
                    }

                    // Calculate CPU Percentage
                    if (totalBefore != 0) {
                        workDiff = work - workBefore;
                        totalDiff = total - totalBefore;
                        cpuUsage = workDiff * 100 / (float) totalDiff;
                    }

                    totalBefore = total;
                    workBefore = work;
                }
            };
        }

        return null;
    }

    /** <p>Returns the memory Uuage for this Android device.</p>
     *
     * @return the memory usage for android.
     */
    @NonNull public double getMemoryUsage() {
        // memInfo.totalMem needs API 16+
        return memInfo.availMem / (double) memInfo.totalMem * 100.0;
    }

    /** <p>Returns the cpu usage for this Android device. Return 0 if Android 8+.</p>
     *
     * @return the cpu usage for android.
     */
    @NonNull public float getCpuUsage() {
        return cpuUsage;
    }

}
