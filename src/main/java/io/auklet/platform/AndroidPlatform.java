package io.auklet.platform;

import android.content.Context;
import android.os.Build;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.platform.metrics.AndroidMetrics;
import net.jcip.annotations.Immutable;
import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** <p>Platform methods specific to Android.</p> */
@Immutable
public final class AndroidPlatform extends AbstractPlatform {

    private final Context context;
    private final AndroidMetrics metrics;

    /**
     * <p>Constructor.</p>
     *
     * @param context the Android context.
     * @throws AukletException if context is {@code null}.
     */
    public AndroidPlatform(@NonNull Object context) throws AukletException {
        if (!(context instanceof Context)) throw new AukletException("Android platform was given a non-Context object.");
        if (Build.VERSION.SDK_INT < 16) throw new AukletException("Unsupported Android API level: " + Build.VERSION.SDK_INT);
        this.context = (Context) context;
        this.metrics = new AndroidMetrics(this.context);
    }

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        super.start(agent);
        Runnable cpuUsage = metrics.calculateCpuUsage();
        if (cpuUsage != null) agent.scheduleRepeatingTask(cpuUsage, 0L, 1L, TimeUnit.SECONDS);
    }

    /**
     * <p>Returns the directory used by the agent on Android to store config files.</p>
     *
     * @param fromConfig unused.
     * @return a list with exactly one element: {@code context.getFilesDir().getPath() + "/.auklet"}.
     */
    @Override public List<String> getPossibleConfigDirs(@Nullable String fromConfig) {
        return Collections.singletonList(this.context.getFilesDir().getPath() + "/.auklet");
    }

    @Override public void addSystemMetrics(@NonNull MessagePacker msgpack) throws AukletException, IOException {
        if (msgpack == null) throw new AukletException("msgpack is null.");
        msgpack.packString("memoryUsage").packDouble(metrics.getMemoryUsage());
        msgpack.packString("cpuUsage").packDouble(metrics.getCpuUsage());
    }

}
