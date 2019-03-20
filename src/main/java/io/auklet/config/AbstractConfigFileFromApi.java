package io.auklet.config;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.AukletException;
import io.auklet.misc.Util;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;

/**
 * <p>Base class of all Auklet agent config files that are sourced from the API.</p>
 *
 * <p>Type {@code T} represents the data type that is returned by the Auklet API, and is also what
 * is used to persist the config file to disk.</p>
 */
@NotThreadSafe
public abstract class AbstractConfigFileFromApi<T> extends AbstractConfigFile {

    /**
     * <p>Loads the config for this object, either from disk or from the API. If the latter, this method
     * persists the API response to disk prior to returning.</p>
     *
     * @return never {@code null}.
     * @throws AukletException if the config cannot be read from disk or fetched from the API, or if it
     * cannot be written to disk.
     */
    @NonNull protected final T loadConfig() throws AukletException {
        T config = this.readFromDisk();
        if (config == null) {
            config = this.fetchFromApi();
            this.writeToDisk(config);
        }
        return config;
    }

    /**
     * <p>Returns the config file contents from disk.</p>
     *
     * @return {@code null} if and only if the file does not exist on disk or could not be read.
     */
    @CheckForNull protected abstract T readFromDisk();

    /**
     * <p>Fetches the config file from the Auklet API.</p>
     *
     * @return never {@code null}.
     * @throws AukletException if there is a problem communicating with the API.
     */
    @NonNull protected abstract T fetchFromApi() throws AukletException;

    /**
     * <p>Writes the config file to disk.</p>
     *
     * @param contents never {@code null}.
     * @throws AukletException if an error occurs while writing the file.
     */
    @NonNull protected abstract void writeToDisk(@NonNull T contents) throws AukletException;

    /**
     * <p>Loads the config file from disk into a string, using the UTF-8 charset.</p>
     *
     * @return never {@code null}. If the file does not exist, an empty string is returned.
     * @throws IOException if the file cannot be read.
     */
    @NonNull protected final String getStringFromDisk() throws IOException {
        try {
            byte[] bytes = Util.read(this.file);
            return new String(bytes, "UTF-8");
        } catch (SecurityException e) {
            throw new IOException(e);
        }
    }

}
