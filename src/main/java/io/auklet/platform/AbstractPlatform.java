package io.auklet.platform;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.Config;
import io.auklet.config.DeviceAuth;
import io.auklet.core.HasAgent;
import io.auklet.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** <p>Provides logic common to all platforms.</p> */
public abstract class AbstractPlatform extends HasAgent implements Platform {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPlatform.class);

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        this.setAgent(agent);
    }

    @CheckForNull @Override public final File obtainConfigDir(@Nullable String fromConfig) {
        // If a directory contains the auth file, use that directory.
        // We don't care if the other files don't exist because we'll create them later if needed.
        LOGGER.debug("Checking directories for existing config files.");
        List<String> configDirs = getPossibleConfigDirs(fromConfig);
        for (String dir : configDirs) {
            File authFile = new File(dir, DeviceAuth.FILENAME);
            try {
                if (authFile.exists()) {
                    LOGGER.debug("Using existing config directory: {}", dir);
                    return new File(dir);
                }
            } catch (SecurityException e) {
                if ( Auklet.LOUD_SECURITY_EXCEPTIONS) LOGGER.warn("Skipping directory '{}' due to an error.", dir, e);
                else LOGGER.warn("Skipping directory '{}' due to an error: {}", dir, e.getMessage());
            }
        }

        LOGGER.debug("No existing config files found; checking directories for suitability.");
        // Use the first directory that we can create.
        for (String dir : configDirs) {
            try {
                return tryDir(new File(dir));
            } catch (SecurityException e) {
                if ( Auklet.LOUD_SECURITY_EXCEPTIONS) LOGGER.warn("Skipping directory '{}' due to an error.", dir, e);
                else LOGGER.warn("Skipping directory '{}' due to an error: {}", dir, e.getMessage());
            } catch (IllegalArgumentException | UnsupportedOperationException | IOException e) {
                LOGGER.warn("Skipping directory '{}' due to an error.", dir, e);
            }
        }
        return null;
    }

    /**
     * <p>Determines the possible config directories that can be used by the agent.</p>
     *
     * @param fromConfig the value from the {@link Config} object, env var and/or JVM sysprop, possibly
     * {@code null}.
     * @return the list of possible config directories that can be used on this platform.
     */
    @NonNull protected abstract List<String> getPossibleConfigDirs(@Nullable String fromConfig);

    /**
     * <p>Checks the directory for write permissions, or attempts to create the directory, or gives up.</p>
     *
     * @param possibleConfigDir the directory that needs to be checked for write permissions.
     * @return possibly {@code null}, in which case the Auklet agent must throw an exception during
     * initialization and all data sent to the agent must be silently discarded.
     */
    private static File tryDir(File possibleConfigDir) throws IOException {
        boolean alreadyExists = possibleConfigDir.exists();
        // Per Javadocs, File.mkdirs() no-ops with no exception if the given path already
        // exists *as a directory*. However, this result does not imply that the JVM has
        // write permissions *inside* the directory, which would be the case only if the
        // directory existed beforehand.
        //
        // To alleviate this, we do a test file write inside the directory *only if the
        // directory existed beforehand*.
        if (alreadyExists) {
            File tempFile = File.createTempFile("auklet", null, possibleConfigDir);
            LOGGER.debug("Using existing config directory: {}", possibleConfigDir.getPath());
            FileUtil.deleteQuietly(tempFile);
            return possibleConfigDir;
        } else if (possibleConfigDir.mkdirs()) {
            LOGGER.debug("Created new config directory: {}", possibleConfigDir.getPath());
            return possibleConfigDir;
        }
        return null;
    }

}
