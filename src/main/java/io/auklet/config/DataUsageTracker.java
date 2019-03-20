package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.misc.AukletDaemonExecutor;
import io.auklet.misc.Util;
import mjson.Json;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p>The <i>data usage tracker file</i> is used to persist between restarts the amount of data that has
 * been sent by the Auklet agent to the sink.</p>
 */
@NotThreadSafe
public final class DataUsageTracker extends AbstractConfigFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataUsageTracker.class);
    private static final String USAGE_FILE = "usage";
    private static final String USAGE_KEY = "usage";

    private final Object lock = new Object();
    private ScheduledFuture<?> currentWriteTask;
    private long bytesSent = 0L;

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        LOGGER.debug("Loading data usage tracker file.");
        super.start(agent);
        try {
            // If the file doesn't exist, create it.
            if (!this.file.exists()) this.writeUsageToDisk(0L);
            // Read from disk.
            byte[] usageBytes = Util.read(this.file);
            String usageString = new String(usageBytes, "UTF-8");
            // Parse the JSON and set relevant fields.
            Json usageJson = Util.validateJson(Util.readJson(usageString), this.getClass().getName());
            this.bytesSent = usageJson.at(USAGE_KEY, 0L).asLong();
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            LOGGER.warn("Could not read data usage tracker file from disk, assuming zero usage.", e);
        }
    }

    @Override public String getName() { return USAGE_FILE; }

    /**
     * <p>Return the number of bytes sent so far.</p>
     *
     * @return the number of bytes sent.
     */
    public long getBytesSent() { return this.bytesSent; }

    /**
     * <p>Adds the input number of bytes to the current amount of bytes sent.</p>
     *
     * @param moreBytes no-op if less than 1.
     */
    public void addMoreData(long moreBytes) {
        if (moreBytes < 1) return;
        this.bytesSent += moreBytes;
        this.saveUsage(this.bytesSent);
    }

    /** <p>Resets the data usage to zero.</p> */
    public void reset() {
        this.bytesSent = 0L;
        this.saveUsage(this.bytesSent);
    }

    /**
     * <p>Asynchronously saves the given usage value to disk.</p>
     *
     * @param givenUsage the usage value to write.
     */
    private void saveUsage(final long givenUsage) {
        try {
            // If there is already a pending write task, cancel it.
            synchronized (this.lock) {
                if (this.currentWriteTask != null) currentWriteTask.cancel(false);
                // Queue the new write task.
                this.currentWriteTask = this.getAgent().scheduleOneShotTask(new AukletDaemonExecutor.CancelSilentlyRunnable() {
                    @Override
                    public void run() {
                        // This task is no longer pending, so clear its status.
                        synchronized (lock) {
                            currentWriteTask = null;
                        }
                        try {
                            writeUsageToDisk(givenUsage);
                        } catch (IOException | SecurityException e) {
                            LOGGER.warn("Could not save data usage to disk.", e);
                        }
                    }
                }, 5, TimeUnit.SECONDS); // 5-second cooldown.
            }
        } catch (AukletException e) {
            LOGGER.warn("Could not queue data usage save task.", e);
        }
    }

    /**
     * <p>Write the usage value to disk.</p>
     *
     * @param usage the current usage value.
     * @throws IOException if an error occurs while writing the file.
     * @throws SecurityException if an error occurs while writing the file.
     */
    private void writeUsageToDisk(long usage) throws IOException {
        Json usageJson = Json.object();
        usageJson.set(USAGE_KEY, usage);
        Util.writeUtf8(this.file, usageJson.toString());
    }

}
