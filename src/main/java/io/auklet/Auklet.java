package io.auklet;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.core.DataUsageMonitor;
import io.auklet.jvm.AukletExceptionHandler;
import io.auklet.config.DeviceAuth;
import io.auklet.core.AukletApi;
import io.auklet.core.Util;
import io.auklet.sink.*;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.jar.Manifest;

/**
 * <p>The entry point for the Auklet agent for Java and related languages/platforms.</p>
 *
 * <p>This class is a singleton; explicit instantiation via reflection provides no advantages over the
 * {@link #init()}/{@link #init(Config)} methods and will throw an exception if reflection is used to
 * attempt to construct a second instance of this class.</p>
 *
 * <p>The <b>only</b> classes/methods in the Auklet agent Javadocs that are officially supported for end
 * users are:</p>
 *
 * <ul>
 *   <li>All {@code public static} methods in the {@link Auklet} class.</li>
 *   <li>All {@code public} methods in the {@link Config} class.</li>
 *   <li>The {@link AukletException} class.</li>
 * </ul>
 *
 * <p><b>Unless instructed to do so by Auklet support, do not use any classes/fields/methods other than
 * those described above.</b></p>
 */
@ThreadSafe
public final class Auklet {

    /** <p>The version of the Auklet agent JAR.</p> */
    public static final String VERSION;
    private static final Logger LOGGER = LoggerFactory.getLogger(Auklet.class);
    private static final Object LOCK = new Object();
    private static Auklet agent = null;

    private final String appId;
    private final String baseUrl;
    private final File configDir;
    private final String serialPort;
    private final int mqttThreads;
    private final String macHash;
    private final String ipAddress;
    private final AukletApi api;
    private final DeviceAuth deviceAuth;
    private final AbstractSink sink;
    private final DataUsageMonitor usageMonitor;
    private final Thread shutdownHook;
    private final ScheduledExecutorService daemon;

    static {
        // Extract Auklet agent version from the manifest.
        String version = "unknown";
        try (InputStream manifestStream = Auklet.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
            if (manifestStream != null) {
                Manifest manifest = new Manifest(manifestStream);
                version = manifest.getMainAttributes().getValue("Implementation-Version");
                version = Util.defaultValue(version, "unknown");
            }
        } catch (SecurityException | IOException e) {
            LOGGER.warn("Could not obtain Auklet agent version from manifest", e);
        }
        VERSION = version;
        // Initialize the Auklet agent if requested via env var or JVM sysprop.
        String fromEnv = System.getenv("AUKLET_AUTO_START");
        String fromProp = System.getProperty("auklet.auto.start");
        if (Boolean.valueOf(fromEnv) || Boolean.valueOf(fromProp)) {
            LOGGER.info("Auto-starting Auklet agent.");
            try {
                init(null);
            } catch (AukletException e) {
                LOGGER.error("Could not auto-start Auklet agent", e);
            }
        }
    }

    /**
     * <p>Auklet agent constructor, called via {@link #init(Config)}.</p>
     *
     * @param config possibly {@code null}.
     * @throws AukletException if the agent cannot be initialized.
     */
    private Auklet(@Nullable Config config) throws AukletException {
        synchronized (LOCK) {
            // We check this in the init method to provide a proper message, in case the user accidentally
            // attempted to init twice. We check again here to prevent instantiation via reflection.
            if (agent != null) throw new AukletException("Use Auklet.init() to initialize the agent");
        }

        if (config == null) config = new Config();
        this.appId = Util.getValue(config.getAppId(), "AUKLET_APP_ID", "auklet.app.id");
        if (Util.isNullOrEmpty(this.appId)) throw new AukletException("App ID is null or empty");
        String apiKey = Util.getValue(config.getApiKey(), "AUKLET_API_KEY", "auklet.api.key");
        if (Util.isNullOrEmpty(apiKey)) throw new AukletException("API key is null or empty");
        String baseUrlMaybeNull = Util.getValue(config.getBaseUrl(), "AUKLET_BASE_URL", "auklet.base.url");
        this.baseUrl = Util.defaultValue(Util.removeTrailingSlash(baseUrlMaybeNull), "https://api.auklet.io");
        boolean autoShutdown = Util.getValue(config.getAutoShutdown(), "AUKLET_AUTO_SHUTDOWN", "auklet.auto.shutdown");
        boolean uncaughtExceptionHandler = Util.getValue(config.getUncaughtExceptionHandler(), "AUKLET_UNCAUGHT_EXCEPTION_HANDLER", "auklet.uncaught.exception.handler");
        this.serialPort = Util.getValue(config.getSerialPort(), "AUKLET_SERIAL_PORT", "auklet.serial.port");
        int internalThreads = Util.getValue(config.getThreads(), "AUKLET_THREADS", "auklet.threads");
        if (internalThreads < 1) internalThreads = 1;
        int mqttThreadsFromConfig = Util.getValue(config.getMqttThreads(), "AUKLET_THREADS_MQTT", "auklet.threads.mqtt");
        if (mqttThreadsFromConfig < 1) mqttThreadsFromConfig = 3;
        this.mqttThreads = mqttThreadsFromConfig;

        this.macHash = Util.getMacAddressHash();
        this.ipAddress = Util.getIpAddress();

        // Finalizing the config dir may cause changes to the filesystem, so we wait to do this
        // until we've validated the rest of the config, in case there is a config error; this
        // approach avoids unnecessary filesystem changes for bad configs.
        this.configDir = obtainConfigDir(config.getConfigDir());
        if (configDir == null) throw new AukletException("Could not find or create any config directory; see previous logged errors for details");

        this.api = new AukletApi(apiKey);
        this.deviceAuth = new DeviceAuth();
        // In the future we may want to make this some kind of SinkFactory.
        if (this.serialPort != null) {
            this.sink = new SerialPortSink();
        } else {
            this.sink = new AukletIoSink();
        }
        this.usageMonitor = new DataUsageMonitor();

        if (autoShutdown) {
            Thread hook = createShutdownHook();
            this.shutdownHook = hook;
            try {
                Runtime.getRuntime().addShutdownHook(hook);
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                throw new AukletException("Could not add JVM shutdown hook", e);
            }
        } else {
            this.shutdownHook = null;
        }
        if (uncaughtExceptionHandler) {
            try {
                Thread.setDefaultUncaughtExceptionHandler(new AukletExceptionHandler());
            } catch (SecurityException e) {
                throw new AukletException("Could not set default uncaught exception handler", e);
            }
        }

        this.daemon = Executors.newScheduledThreadPool(internalThreads, Util.createDaemonThreadFactory());
    }

    /**
     * <p>Initializes the agent with all configuration options specified via environment variables
     * and/or JVM system properties.</p>
     *
     * <p>If an exception is thrown by this method, all data submission methods are guaranteed to
     * silently no-op.</p>
     *
     * @throws AukletException if the agent cannot be initialized.
     * @throws IllegalArgumentException if the app ID and/or API key is either {@code null} or empty, or if
     * no config directory cannot be obtained.
     */
    public static void init() throws AukletException {
        init(null);
    }

    /**
     * <p>Initializes the agent with the given configuration values, falling back on environment
     * variables, JVM system properties and/or default values where needed.</p>
     *
     * <p>If an exception is thrown by this method, all data submission methods are guaranteed to
     * silently no-op.</p>
     *
     * @param config the agent config object. May be {@code null}.
     * @throws AukletException if the agent cannot be initialized.
     */
    public static void init(@Nullable Config config) throws AukletException {
        synchronized (LOCK) {
            // We check this here to provide a proper message, in case the user accidentally attempted to
            // init twice. We check again in the constructor to prevent instantiation via reflection.
            if (agent != null) throw new AukletException("Agent is already initialized; use Auklet.shutdown() first");
            agent = new Auklet(config);
            try {
                agent.start();
            } catch (AukletException e) {
                shutdown();
                throw e;
            }
        }
    }

    /**
     * <p>Sends the given throwable to the agent as an <i>event</i>.</p>
     *
     * @param throwable if {@code null}, this method is no-op.
     */
    public static void send(@Nullable Throwable throwable) {
        if (throwable == null) return;
        synchronized (LOCK) {
            if (agent == null) return;
            agent.doSend(throwable);
        }
    }

    /**
     * <p>Shuts down the agent and closes/disconnects from any underlying resources. Calling this method more
     * than once has no effect; therefore, explicitly calling this method when the agent has been initialized
     * with a builtin JVM shutdown hook is unnecessary, unless you wish to shutdown the Auklet agent earlier
     * than JVM shutdown.</p>
     */
    public static void shutdown() {
        synchronized (LOCK) {
            if (agent == null) return;
            agent.doShutdown(false);
            agent = null;
        }
    }

    /**
     * <p>Returns the app ID for this instance of the agent.</p>
     *
     * @return never {@code null} or empty.
     */
    @NonNull public String getAppId() {
        return this.appId;
    }

    /**
     * <p>Returns the API base URL for this instance of the agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public String getBaseUrl() {
        return this.baseUrl;
    }

    /**
     * <p>Returns the config directory for this instance of the agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public File getConfigDir() {
        return this.configDir;
    }

    /**
     * <p>Returns the serial port that will be used by this instance of the agent.</p>
     *
     * @return possibly {@code null}, in which case a serial port will not be used.
     */
    @CheckForNull public String getSerialPort() {
        return this.serialPort;
    }

    /**
     * <p>Returns the number of MQTT threads that will be used by this instance of the agent.</p>
     *
     * @return never less than 1.
     */
    public int getMqttThreads() { return this.mqttThreads; }

    /**
     * <p>Returns the MAC address hash for this instance of the agent.</p>
     *
     * @return never {@code null} or empty.
     */
    @NonNull public String getMacHash() {
        return this.macHash;
    }

    /**
     * <p>Returns the public IP address for this instance of the agent.</p>
     *
     * @return never {@code null} or empty.
     */
    @NonNull public String getIpAddress() {
        return this.ipAddress;
    }

    /**
     * <p>Returns the API object for this instance of the agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public AukletApi getApi() {
        return this.api;
    }

    /**
     * <p>Returns the device auth for this instance of the agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public DeviceAuth getDeviceAuth() {
        return this.deviceAuth;
    }

    /**
     * <p>Returns the data usage limit for this instance of the agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public DataUsageMonitor getUsageMonitor() {
        return this.usageMonitor;
    }

    /**
     * <p>Schedules the given one-shot task to run on the Auklet agent's daemon executor thread.</p>
     *
     * @param command the task to execute.
     * @param delay the time from now to delay execution.
     * @param unit the time unit of the delay parameter.
     * @return never {@code null}.
     * @throws AukletException to wrap any underlying exceptions.
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
     */
    @NonNull public ScheduledFuture<?> scheduleOneShotTask(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) throws AukletException {
        if (command == null) throw new AukletException("Daemon task is null");
        if (unit == null) throw new AukletException("Daemon task time unit is null");
        try {
            return this.daemon.schedule(command, delay, unit);
        } catch (RejectedExecutionException e) {
            throw new AukletException("Could not schedule one-shot task", e);
        }
    }

    /**
     * <p>Schedules the given task to run on the Auklet agent's daemon executor thread.</p>
     *
     * @param command the task to execute.
     * @param initialDelay the time to delay first execution.
     * @param period the period between successive executions.
     * @param unit the time unit of the initialDelay and period parameters.
     * @return never {@code null}.
     * @throws AukletException to wrap any underlying exceptions.
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
     */
    @NonNull public ScheduledFuture<?> scheduleRepeatingTask(@NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) throws AukletException {
        if (command == null) throw new AukletException("Daemon task is null");
        if (unit == null) throw new AukletException("Daemon task time unit is null");
        try {
            return this.daemon.scheduleAtFixedRate(command, initialDelay, period, unit);
        } catch (RejectedExecutionException | IllegalArgumentException e) {
            throw new AukletException("Could not schedule repeating task", e);
        }
    }

    /**
     * <p>Creates a JVM shutdown thread that shuts down the Auklet agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull private static Thread createShutdownHook() {
        return new Thread() {
            @Override public void run() {
                synchronized (LOCK) {
                    if (agent != null) {
                        try {
                            agent.doShutdown(true);
                            agent = null;
                        } catch (Exception e) {
                            // Because this is a shutdown hook thread, we want to make sure we intercept
                            // any kind of exception and log it for the benefit of the end-user.
                            LOGGER.warn("Error while shutting down Auklet agent", e);
                        }
                    }
                }
            }
        };
    }

    /**
     * <p>Returns the directory the Auklet agent will use to store its configuration files. This method
     * creates/tests write access to the target config directory after determining which directory to use,
     * per the logic described in the class-level Javadoc.</p>
     *
     * @param fromConfigObject the value from the {@link Config config object}, possibly
     * {@code null}.
     * @return possibly {@code null}, in which case the Auklet agent must throw an exception during
     * initialization and all data sent to the agent must be silently discarded.
     */
    @CheckForNull private static File obtainConfigDir(@Nullable String fromConfigObject) {
        // Consider config dir settings in this order.
        List<String> possibleConfigDirs = Arrays.asList(
                fromConfigObject,
                System.getenv("AUKLET_CONFIG_DIR"),
                System.getProperty("auklet.config.dir"),
                System.getProperty("user.dir"),
                System.getProperty("user.home"),
                System.getProperty("java.io.tmpdir")
        );
        // Drop any env vars/sysprops whose value is null, and append the auklet subdir to each remaining value.
        List<String> filteredConfigDirs = new ArrayList<>();
        for (String dir : possibleConfigDirs) {
            if (!Util.isNullOrEmpty(dir)) filteredConfigDirs.add(Util.removeTrailingSlash(dir) + "/aukletFiles");
        }
        // If a directory contains the auth file, use that directory.
        // We don't care if the other files don't exist because we'll create them later if needed.
        for (String dir : filteredConfigDirs) {
            File authFile = new File(dir, DeviceAuth.FILENAME);
            try {
                if (authFile.exists()) {
                    LOGGER.info("Using existing config directory: {}", dir);
                    return new File(dir);
                }
            } catch (SecurityException e) {
                LOGGER.warn("Skipping directory {} due to an error", dir, e);
            }
        }
        // No existing config files were found. Use the first directory that we can create.
        for (String dir : filteredConfigDirs) {
            File possibleConfigDir = new File(dir);
            try {
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
                    LOGGER.info("Using existing config directory: {}", dir);
                    Util.deleteQuietly(tempFile);
                } else if (possibleConfigDir.mkdirs()) {
                    LOGGER.info("Created new config directory: {}", dir);
                }
                return possibleConfigDir;
            } catch (IllegalArgumentException | UnsupportedOperationException | IOException | SecurityException e) {
                LOGGER.warn("Skipping directory {} due to an error", dir, e);
            }
        }
        return null;
    }

    /**
     * <p>Starts the Auklet agent by:</p>
     *
     * <ul>
     *     <li>Passing the Auklet agent reference to internal objects that require it.</li>
     *     <li>Loading configuration files from disk.</li>
     *     <li>Starting the data sink selected by the agent configuration.</li>
     *     <li>Starting the data usage monitor daemon.</li>
     * </ul>
     *
     * @throws AukletException if the underlying resources cannot be started.
     */
    private void start() throws AukletException {
        this.deviceAuth.start(this);
        this.sink.start(this);
        this.usageMonitor.start(this);
    }

    /**
     * <p>Queues a task to submit the given throwable to the data sink.</p>
     *
     * @param throwable if {@code null}, this method is no-op.
     */
    private void doSend(@Nullable final Throwable throwable) {
        if (throwable == null) return;
        try {
            this.scheduleOneShotTask(new Runnable() {
                @Override public void run() {
                    try {
                        sink.send(throwable);
                    } catch (AukletException e) {
                        LOGGER.warn("Could not send event", e);
                    }
                }
            }, 0, TimeUnit.SECONDS); // 5-second cooldown.
        } catch (AukletException e) {
            LOGGER.warn("Could not queue event send task", e);
        }
    }

    /**
     * <p>Shuts down the Auklet agent.</p>
     *
     * @param viaJvmHook {@code true} if shutdown is occurring due to a JVM hook, {@code false} otherwise.
     */
    private void doShutdown(boolean viaJvmHook) {
        LOGGER.info("Auklet agent is shutting down.");
        boolean jvmHookIsShuttingDown = this.shutdownHook != null && viaJvmHook;
        if (!jvmHookIsShuttingDown) Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        this.sink.shutdown();
        try {
            this.daemon.shutdown();
            if (!this.daemon.awaitTermination(3, TimeUnit.SECONDS)) this.daemon.shutdownNow();
        } catch (InterruptedException ie) {
            // End-users that call shutdown() explicitly should only do so inside the context of a JVM shutdown.
            // Thus, rethrowing this exception creates unnecessary noise and clutters the API/Javadocs.
            LOGGER.warn("Interrupted while awaiting Auklet agent thread pool shutdown.");
            this.daemon.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (SecurityException se) {
            LOGGER.warn("Could not shut down Auklet agent thread pool", se);
        }
        this.api.shutdown();
    }

}
