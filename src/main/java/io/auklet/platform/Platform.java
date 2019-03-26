package io.auklet.platform;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;
import io.auklet.Config;
import org.msgpack.core.MessagePacker;

import java.io.File;
import java.io.IOException;

/**
 * <p>The platform on which Auklet is running. After initialization, the agent has a reference to exactly
 * one instance of this interface that provides behavior appropriate to the underlying platform (Java SE,
 * Android, etc.)</p>
 */
public interface Platform {

    /**
     * <p>Adds OS memory/CPU usage metrics to the given MessagePack as map entries.</p>
     *
     * @param msgpack the MessagePack to which the metrics will be added.
     * @throws AukletException if the input is {@code null}.
     * @throws IOException if an error occurs while adding the metrics.
     */
    void addSystemMetrics(@NonNull MessagePacker msgpack) throws AukletException, IOException;

    /**
     * <p>Returns the directory the Auklet agent will use to store its configuration files. This method
     * creates/tests write access to the target config directory after determining which directory to use,
     * per the logic described in the class-level Javadoc.</p>
     *
     * @param fromConfig the value from the {@link Config} object, env var and/or JVM sysprop, possibly
     * {@code null}.
     * @return possibly {@code null}, in which case the Auklet agent must throw an exception during
     * initialization and all data sent to the agent must be silently discarded.
     */
    @CheckForNull File obtainConfigDir(@Nullable String fromConfig);

}
