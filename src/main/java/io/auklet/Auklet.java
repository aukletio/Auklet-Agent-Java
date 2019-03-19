package io.auklet;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.misc.AukletDaemonExecutor;
import io.auklet.core.DataUsageMonitor;
import io.auklet.core.AukletExceptionHandler;
import io.auklet.config.DeviceAuth;
import io.auklet.core.AukletApi;
import io.auklet.misc.Util;
import io.auklet.platform.AbstractPlatform;
import io.auklet.platform.AndroidPlatform;
import io.auklet.platform.JavaPlatform;
import io.auklet.platform.Platform;
import io.auklet.sink.*;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private static final ScheduledExecutorService DAEMON = new AukletDaemonExecutor(1, Util.createDaemonThreadFactory("Auklet"));
    private static Auklet agent = null;

    private final String appId;
    private final String baseUrl;
    private final AbstractPlatform platform;
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

    static {
        // Extract Auklet agent version from the manifest.
        String version = "unknown";
        try (InputStream manifestStream = Auklet.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
            if (manifestStream != null) {
                Manifest manifest = new Manifest(manifestStream);
                version = manifest.getMainAttributes().getValue("Implementation-Version");
                version = Util.orElse(version, "unknown");
            }
            LOGGER.info("Auklet Agent version {}", version);
        } catch (SecurityException | IOException e) {
            LOGGER.warn("Could not obtain Auklet agent version from manifest.", e);
        }
        VERSION = version;
        // Initialize the Auklet agent if requested via env var or JVM sysprop.
        String fromEnv = System.getenv("AUKLET_AUTO_START");
        String fromProp = System.getProperty("auklet.auto.start");
        if (Boolean.valueOf(fromEnv) || Boolean.valueOf(fromProp)) {
            LOGGER.info("Auto-start requested.");
            init(null);
        }
    }

    /**
     * <p>Auklet agent constructor, called via {@link #init(Config)}.</p>
     *
     * @param config possibly {@code null}.
     * @throws IllegalStateException if the agent is already initialized.
     * @throws AukletException if the agent cannot be initialized.
     */
    private Auklet(@Nullable Config config) throws AukletException {
        synchronized (LOCK) {
            // We check this in the init method to provide a proper message, in case the user accidentally
            // attempted to init twice. We check again here to prevent instantiation via reflection.
            if (agent != null) throw new IllegalStateException("Use Auklet.init() to initialize the agent.");
        }

        LOGGER.debug("Parsing configuration.");
        if (config == null) config = new Config();

        this.appId = Util.getValue(config.getAppId(), "AUKLET_APP_ID", "auklet.app.id");
        String apiKey = Util.getValue(config.getApiKey(), "AUKLET_API_KEY", "auklet.api.key");
        if (Util.isNullOrEmpty(this.appId)) throw new AukletException("App ID is null or empty.");
        if (Util.isNullOrEmpty(apiKey)) throw new AukletException("API key is null or empty.");

        String baseUrlMaybeNull = Util.getValue(config.getBaseUrl(), "AUKLET_BASE_URL", "auklet.base.url");
        this.baseUrl = Util.orElse(Util.removeTrailingSlash(baseUrlMaybeNull), "https://api.auklet.io");
        LOGGER.info("Base URL: {}", this.baseUrl);

        Boolean autoShutdownMaybeNull = Util.getValue(config.getAutoShutdown(), "AUKLET_AUTO_SHUTDOWN", "auklet.auto.shutdown");
        Boolean uncaughtExceptionHandlerMaybeNull = Util.getValue(config.getUncaughtExceptionHandler(), "AUKLET_UNCAUGHT_EXCEPTION_HANDLER", "auklet.uncaught.exception.handler");
        boolean autoShutdown = autoShutdownMaybeNull == null ? true : autoShutdownMaybeNull;
        boolean uncaughtExceptionHandler = uncaughtExceptionHandlerMaybeNull == null ? true : uncaughtExceptionHandlerMaybeNull;

        this.serialPort = Util.getValue(config.getSerialPort(), "AUKLET_SERIAL_PORT", "auklet.serial.port");
        Object androidContext = config.getAndroidContext();
        if (androidContext != null && serialPort != null) throw new AukletException("Auklet can not use serial port when on an Android platform.");

        Integer mqttThreadsFromConfigMaybeNull = Util.getValue(config.getMqttThreads(), "AUKLET_THREADS_MQTT", "auklet.threads.mqtt");
        int mqttThreadsFromConfig = mqttThreadsFromConfigMaybeNull == null ? 3 : mqttThreadsFromConfigMaybeNull;
        if (mqttThreadsFromConfig < 1) mqttThreadsFromConfig = 3;
        this.mqttThreads = mqttThreadsFromConfig;

        LOGGER.debug("Getting IP/MAC address.");
        this.macHash = Util.getMacAddressHash();
        this.ipAddress = Util.getIpAddress();

        // Finalizing the config dir may cause changes to the filesystem, so we wait to do this
        // until we've validated the rest of the config, in case there is a config error; this
        // approach avoids unnecessary filesystem changes for bad configs.
        LOGGER.debug("Determining which config directory to use.");
        if (androidContext == null) {
            this.platform = new JavaPlatform();
        } else {
            this.platform = new AndroidPlatform(androidContext);
        }
        this.configDir = platform.obtainConfigDir(Util.getValue(config.getConfigDir(), "AUKLET_CONFIG_DIR", "auklet.config.dir"));
        if (configDir == null) throw new AukletException("Could not find or create any config directory; see previous logged errors for details");

        LOGGER.debug("Configuring agent resources.");
        this.api = new AukletApi(apiKey);
        this.deviceAuth = new DeviceAuth();

        // In the future we may want to make this some kind of SinkFactory.
        if (this.serialPort != null) {
            this.sink = new SerialPortSink();
        } else {
            this.sink = new AukletIoSink();
        }
        this.usageMonitor = new DataUsageMonitor();

        LOGGER.debug("Configuring JVM integrations.");
        if (autoShutdown) {
            Thread hook = createShutdownHook();
            this.shutdownHook = hook;
            try {
                Runtime.getRuntime().addShutdownHook(hook);
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                throw new AukletException("Could not add JVM shutdown hook.", e);
            }
        } else {
            this.shutdownHook = null;
        }

        if (uncaughtExceptionHandler) {
            try {
                Thread.setDefaultUncaughtExceptionHandler(new AukletExceptionHandler());
            } catch (SecurityException e) {
                throw new AukletException("Could not set default uncaught exception handler.", e);
            }
        }
    }

    /**
     * <p>Initializes the agent with all configuration options specified via environment variables
     * and/or JVM system properties.</p>
     *
     * <p>Any error that causes the agent to fail to initialize will be logged automatically.</p>
     *
     * @return {@code true} if the agent was initialized successfully, {@code false} otherwise.
     */
    @NonNull public static Future<Boolean> init() {
        return init(null);
    }

    /**
     * <p>Initializes the agent with the given configuration values, falling back on environment
     * variables, JVM system properties and/or default values where needed.</p>
     *
     * <p>Any error that causes the agent to fail to initialize will be logged automatically.</p>
     *
     * @param config the agent config object. May be {@code null}.
     * @return a future whose result is never {@code null}, and is either {@code true} if the agent was
     * initialized successfully or {@code false} otherwise.
     */
    @NonNull public static Future<Boolean> init(@Nullable final Config config) {
        LOGGER.debug("Scheduling init task.");
        Callable<Boolean> initTask = new Callable<Boolean>() {
            @NonNull @Override public Boolean call() {
                synchronized (LOCK) {
                    // We check this here to provide a proper message, in case the user accidentally attempted to
                    // init twice. We check again in the constructor to prevent instantiation via reflection.
                    if (agent != null) {
                        LOGGER.error("Agent is already initialized; use Auklet.shutdown() first.");
                        return false;
                    }
                    LOGGER.info("Starting agent.");
                    try {
                        agent = new Auklet(config);
                        agent.start();
                        LOGGER.info("Agent started successfully.");
                        return true;
                    } catch (Exception e) {
                        // Catch everything so that even programming errors result in an orderly
                        // shutdown of the agent.
                        shutdown();
                        LOGGER.error("Could not start agent.", e);
                        return false;
                    }
                }
            }
        };
        try {
            return DAEMON.submit(initTask);
        } catch (RejectedExecutionException e) {
            FutureTask<Boolean> future = new FutureTask<>(new Runnable() {@Override public void run() { /* no-op */ }}, false);
            future.run();
            LOGGER.error("Could not init agent.", e);
            return future;
        }
    }

    /**
     * <p>Sends the given throwable to the agent as an <i>event</i>.</p>
     *
     * @param throwable if {@code null}, this method is no-op.
     */
    public static void send(@Nullable final Throwable throwable) {
        if (throwable == null) {
            LOGGER.debug("Ignoring send request for null throwable.");
            return;
        }
        LOGGER.debug("Scheduling send task.");
        Runnable sendTask = new Runnable() {
            @Override public void run() {
                synchronized (LOCK) {
                    if (agent == null) {
                        LOGGER.debug("Ignoring send request because agent is null.");
                        return;
                    }
                    agent.doSend(throwable);
                }
            }
        };
        try {
            DAEMON.submit(sendTask);
        } catch (RejectedExecutionException e) {
            LOGGER.error("Could not send event.", e);
        }
    }

    /**
     * <p>Shuts down the agent and closes/disconnects from any underlying resources. Calling this method more
     * than once has no effect; therefore, explicitly calling this method when the agent has been initialized
     * with a builtin JVM shutdown hook is unnecessary, unless you wish to shutdown the Auklet agent earlier
     * than JVM shutdown.</p>
     *
     * <p>Any error that occurs during shutdown will be logged automatically.</p>
     *
     * @return a future whose result is always {@code null}.
     */
    @NonNull public static Future<Object> shutdown() {
        LOGGER.debug("Scheduling shutdown task.");
        Runnable shutdownTask = new Runnable() {
            @Override public void run() {
                synchronized (LOCK) {
                    if (agent == null) {
                        LOGGER.debug("Ignoring shutdown request because agent is null.");
                        return;
                    }
                    agent.doShutdown(false);
                    agent = null;
                }
            }
        };
        try {
            return DAEMON.submit(shutdownTask, null);
        } catch (RejectedExecutionException e) {
            FutureTask<Object> future = new FutureTask<>(new Runnable() {@Override public void run() { /* no-op */ }}, null);
            future.run();
            LOGGER.error("Could not shutdown agent.", e);
            return future;
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
     * <p>Returns the platform for this instance of the agent.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public Platform getPlatform() {
        return this.platform;
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
    @NonNull public ScheduledFuture<?> scheduleOneShotTask(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) throws AukletException { //NOSONAR
        if (command == null) throw new AukletException("Daemon task is null.");
        if (unit == null) throw new AukletException("Daemon task time unit is null.");
        try {
            return DAEMON.schedule(command, delay, unit);
        } catch (RejectedExecutionException e) {
            throw new AukletException("Could not schedule one-shot task.", e);
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
    @NonNull public ScheduledFuture<?> scheduleRepeatingTask(@NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) throws AukletException { //NOSONAR
        if (command == null) throw new AukletException("Daemon task is null.");
        if (unit == null) throw new AukletException("Daemon task time unit is null.");
        try {
            return DAEMON.scheduleAtFixedRate(command, initialDelay, period, unit);
        } catch (RejectedExecutionException | IllegalArgumentException e) {
            throw new AukletException("Could not schedule repeating task.", e);
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
                            LOGGER.warn("Error while shutting down Auklet agent.", e);
                        }
                    }
                }
            }
        };
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
        LOGGER.debug("Starting internal resources.");
        this.deviceAuth.start(this);
        this.usageMonitor.start(this);
        this.platform.start(this);
        this.sink.start(this);
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
                        LOGGER.debug("Sending event for exception: {}", throwable.getClass().getName());
                        sink.send(throwable);
                    } catch (AukletException e) {
                        LOGGER.warn("Could not send event.", e);
                    }
                }
            }, 0, TimeUnit.SECONDS); // 5-second cooldown.
        } catch (AukletException e) {
            LOGGER.warn("Could not queue event send task.", e);
        }
    }

    /**
     * <p>Shuts down the Auklet agent.</p>
     *
     * @param viaJvmHook {@code true} if shutdown is occurring due to a JVM hook, {@code false} otherwise.
     */
    private void doShutdown(boolean viaJvmHook) {
        LOGGER.info("Shutting down agent.");
        boolean jvmHookIsShuttingDown = this.shutdownHook != null && viaJvmHook;
        if (!jvmHookIsShuttingDown) Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        this.sink.shutdown();
        this.api.shutdown();
    }

}
