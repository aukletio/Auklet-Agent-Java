package io.auklet.platform;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.misc.Util;
import io.auklet.platform.metrics.OSMX;
import org.msgpack.core.MessagePacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>This class contain Java specific helper functions.</p>
 */
public class JavaPlatform extends AbstractPlatform {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaPlatform.class);

    @Override public List<String> getPossibleConfigDirs(@Nullable String fromConfig) {
        if (Util.isNullOrEmpty(fromConfig)) LOGGER.warn("Config dir not defined, will attempt to fallback on JVM system properties.");

        // Consider config dir settings in this order.
        List<String> possibleConfigDirs = Arrays.asList(
                fromConfig,
                System.getProperty("user.dir"),
                System.getProperty("user.home"),
                System.getProperty("java.io.tmpdir")
        );

        // Drop any env vars/sysprops whose value is null, and append the auklet subdir to each remaining value.
        List<String> filteredConfigDirs = new ArrayList<>();
        for (String dir : possibleConfigDirs) {
            if (!Util.isNullOrEmpty(dir)) filteredConfigDirs.add(Util.removeTrailingSlash(dir) + "/aukletFiles");
        }
        return filteredConfigDirs;
    }

    @Override public void addSystemMetrics(@NonNull MessagePacker msgpack) throws IOException {
        // Calculate memory usage.
        double memUsage;
        long freeMem = OSMX.BEAN.getFreePhysicalMemorySize();
        long totalMem = OSMX.BEAN.getTotalPhysicalMemorySize();
        if (freeMem >= 0 && totalMem >= 0) {
            memUsage = 100 * (1 - ((double) freeMem / (double) totalMem));
        } else {
            memUsage = 0d;
        }
        msgpack.packString("memoryUsage").packDouble(memUsage);

        // Calculate CPU usage.
        double cpuUsage;
        double loadAvg = OSMX.BEAN.getSystemLoadAverage();
        int processors = OSMX.BEAN.getAvailableProcessors();
        if (loadAvg >= 0 && processors > 0) {
            cpuUsage = 100 * (loadAvg / processors);
        } else {
            cpuUsage = 0d;
        }
        msgpack.packString("cpuUsage").packDouble(cpuUsage);
    }

}
