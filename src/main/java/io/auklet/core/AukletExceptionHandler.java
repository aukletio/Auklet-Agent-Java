package io.auklet.core;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.Auklet;
import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <p>This class sends all uncaught exceptions, except {@link ThreadDeath}, to Auklet.</p> */
@Immutable
public final class AukletExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AukletExceptionHandler.class);

    @Override public void uncaughtException(@Nullable Thread t, @Nullable Throwable e) {
        if (e == null) return;
        if (!(e instanceof ThreadDeath)) {
            LOGGER.debug("Sending uncaught exception.");
            Auklet.send(e);
        }
    }

}
