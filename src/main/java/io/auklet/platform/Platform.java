package io.auklet.platform;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.Config;
import org.msgpack.core.MessagePacker;

import java.io.File;
import java.io.IOException;

/**
 * <p>The platform in which Auklet will be running on.</p>
 */
public interface Platform {

    /**
     * <p>Adds JVM memory and cpu usage current position in the given MessagePacker as a map object.</p>
     *
     * @param msgpack the msgpack that will be sent to auklet
     * @throws IOException if the MessagePacker is {@code null}.
     */
    void addSystemMetrics(@NonNull MessagePacker msgpack) throws IOException;

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
