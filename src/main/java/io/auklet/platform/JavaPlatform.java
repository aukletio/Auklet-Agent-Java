package io.auklet.platform;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;
import io.auklet.misc.Util;
import io.auklet.platform.metrics.JavaMetrics;
import net.jcip.annotations.Immutable;
import org.msgpack.core.MessagePacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** <p>Platform methods specific to Java SE (and variants).</p> */
@Immutable
public final class JavaPlatform extends AbstractPlatform {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaPlatform.class);
    public final JavaMetrics metrics;

    public JavaPlatform() {
        metrics = new JavaMetrics(agent);
    }

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
            if (!Util.isNullOrEmpty(dir)) filteredConfigDirs.add(Util.removeTrailingSlash(dir) + "/.auklet");
        }
        return filteredConfigDirs;
    }

    @Override public void addSystemMetrics(@NonNull MessagePacker msgpack) throws AukletException, IOException {
        if (msgpack == null) throw new AukletException("msgpack is null.");
        msgpack.packString("memoryUsage").packDouble(metrics.getMemoryUsage());
        msgpack.packString("cpuUsage").packDouble(metrics.getCpuUsage());
    }

}
