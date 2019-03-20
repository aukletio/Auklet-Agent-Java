package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.core.HasAgent;
import net.jcip.annotations.NotThreadSafe;

import java.io.File;

/**
 * <p>Descendants of this class represent a configuration file located inside the Auklet agent's
 * configuration directory.</p>
 */
@NotThreadSafe
public abstract class AbstractConfigFile extends HasAgent {

    protected File file;

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        this.setAgent(agent);
        this.file = new File(agent.getConfigDir(), this.getName());
    }

    /**
     * <p>Returns the name of the config file.</p>
     *
     * @return never {@code null}.
     */
    @NonNull protected abstract String getName();

}
