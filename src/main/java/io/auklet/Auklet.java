package io.auklet;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.core.AukletDaemonExecutor;
import io.auklet.core.DataUsageMonitor;
import io.auklet.core.AukletExceptionHandler;
import io.auklet.config.DeviceAuth;
import io.auklet.net.Https;
import io.auklet.util.SysUtil;
import io.auklet.util.ThreadUtil;
import io.auklet.util.Util;
import io.auklet.platform.AbstractPlatform;
import io.auklet.platform.AndroidPlatform;
import io.auklet.platform.JavaPlatform;
import io.auklet.platform.Platform;
import io.auklet.sink.*;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.*;

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

    /** <p>The version of the Auklet agent JAR; never {@code null} or empty.</p> */
    public static final String VERSION;
    /** <p>If {@code false} (default), some SecurityExceptions will be silenced and others will have their stack traces suppressed.</p> */
    public static final boolean LOUD_SECURITY_EXCEPTIONS;
    private static final Logger LOGGER = LoggerFactory.getLogger(Auklet.class);
    private static final Object LOCK = new Object();
    private static final AukletDaemonExecutor DAEMON = new AukletDaemonExecutor(1, ThreadUtil.createDaemonThreadFactory("Auklet"));
    private static final String INVALID_INIT_MSG = "Use Auklet.init() to initialize the agent.";
    @GuardedBy("LOCK") private static Auklet agent = null;

    private final String appId;
    private final String apiKey;
    private final String baseUrl;
    private final AbstractPlatform platform;
    private final File configDir;
    private final String serialPort;
    private final int mqttThreads;
    private final String macHash;
    private final String ipAddress;
    private final Https https;
    private final DeviceAuth deviceAuth;
    private final AbstractSink sink;
    private final DataUsageMonitor usageMonitor;
    private final Thread shutdownHook;

    static {
        // Extract Auklet agent version from the BuildConfig class.
        String version = "unknown";
        try {
            version = BuildConfig.VERSION;
        } catch (RuntimeException | NoClassDefFoundError e) {
            LOGGER.warn("Could not obtain Auklet agent version from manifest.", e);
        }
        LOGGER.info("Auklet Agent version {}", version);
        VERSION = version;
        // Determine if logging SecurityExceptions loudly is allowed (false by default).
        //
        // If this results in its own SecurityExceptions, this means that the end user has a security
        // policy in place that will not allow the end user to turn this feature on. This would make
        // it harder for the end user to debug what the security policy is missing in order for the
        // agent to work per the end user's requirements, so as a courtesy we will always log these
        // failures, but we will never log any other failures.
        //
        // Given the uniqueness of this check, we need to log a custom warning here, so we do not
        // use SysUtil.getValue().
        String fromEnv = null;
        boolean blockedEnv = false;
        try {
            fromEnv = System.getenv("AUKLET_LOUD_SECURITY_EXCEPTIONS");
        } catch (SecurityException e) {
            blockedEnv = true;
        }
        String fromProp = null;
        boolean blockedProp = false;
        try {
            fromProp = System.getProperty("auklet.loud.security.exceptions");
        } catch (SecurityException e) {
            blockedProp = true;
        }
        LOUD_SECURITY_EXCEPTIONS = Boolean.valueOf(fromEnv) || Boolean.valueOf(fromProp);
        if (LOUD_SECURITY_EXCEPTIONS) {
            LOGGER.info("SecurityException loud logging is enabled.");
        } else if (blockedEnv && blockedProp) {
            // We only log the error if the end result is not true. This implies that exactly
            // one of the two calls resulted in a SecurityException and that the other call
            // was successful and returned true, so there would be no reason to log anything.
            LOGGER.warn("JVM security manager prevented checking for 'loud security exceptions' flag!");
        }
        // Initialize the Auklet agent if requested via env var or JVM sysprop.
        boolean autoStart = Boolean.valueOf(SysUtil.getValue((String) null, "AUKLET_AUTO_START", "auklet.auto.start", LOUD_SECURITY_EXCEPTIONS));
        if (autoStart) {
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
            if (agent != null) throw new IllegalStateException(INVALID_INIT_MSG);
        }

        // We now attempt to verify who is invoking the constructor, to try to prevent instantiation.
        // The first frame is the Util method and the next two frames are from the constructor, so we
        // want to inspect the 4th frame.
        //
        // According to Oracle Javadocs, JVMs may sometimes return empty stack traces. If this happens,
        // we have no choice but to skip this check, so warn the end user.
        boolean verified = true;
        StackTraceElement[] st = Util.getCurrentStackTrace();
        if (st.length >= 4) {
            String caller = st[3].getClassName();
            // If it's null or empty, we have to assume the call is legitimate, so warn the end user.
            boolean callerIsValid = !Util.isNullOrEmpty(caller);
            if (!callerIsValid) verified = false;
            else if (!caller.startsWith(Auklet.class.getName())) {
                // Invalid caller, so must be external reflection.
                throw new IllegalStateException(INVALID_INIT_MSG);
            }
        } else verified = false;
        if (!verified) LOGGER.warn("Insufficient information provided by the JVM to validate Auklet constructor call. If you see this warning more than once per JVM execution, you are likely the target of a reflection attack!");

        LOGGER.debug("Parsing configuration.");
        if (config == null) config = new Config();

        this.appId = SysUtil.getValue(config.getAppId(), "AUKLET_APP_ID", "auklet.app.id", LOUD_SECURITY_EXCEPTIONS);
        if (Util.isNullOrEmpty(this.appId)) throw new AukletException("App ID is null or empty.");
        this.apiKey = SysUtil.getValue(config.getApiKey(), "AUKLET_API_KEY", "auklet.api.key", LOUD_SECURITY_EXCEPTIONS);
        if (Util.isNullOrEmpty(this.apiKey)) throw new AukletException("API key is null or empty.");

        String baseUrlMaybeNull = SysUtil.getValue(config.getBaseUrl(), "AUKLET_BASE_URL", "auklet.base.url", LOUD_SECURITY_EXCEPTIONS);
        this.baseUrl = Util.orElse(Util.removeTrailingSlash(baseUrlMaybeNull), "https://api.auklet.io");
        LOGGER.info("Base URL: {}", this.baseUrl);

        Boolean autoShutdownMaybeNull = SysUtil.getValue(config.getAutoShutdown(), "AUKLET_AUTO_SHUTDOWN", "auklet.auto.shutdown", LOUD_SECURITY_EXCEPTIONS);
        Boolean uncaughtExceptionHandlerMaybeNull = SysUtil.getValue(config.getUncaughtExceptionHandler(), "AUKLET_UNCAUGHT_EXCEPTION_HANDLER", "auklet.uncaught.exception.handler", LOUD_SECURITY_EXCEPTIONS);
        boolean autoShutdown = autoShutdownMaybeNull == null ? true : autoShutdownMaybeNull;
        boolean uncaughtExceptionHandler = uncaughtExceptionHandlerMaybeNull == null ? true : uncaughtExceptionHandlerMaybeNull;

        this.serialPort = SysUtil.getValue(config.getSerialPort(), "AUKLET_SERIAL_PORT", "auklet.serial.port", LOUD_SECURITY_EXCEPTIONS);
        Object androidContext = config.getAndroidContext();
        if (androidContext != null && serialPort != null) throw new AukletException("Auklet can not use serial port when on an Android platform.");

        Integer mqttThreadsFromConfigMaybeNull = SysUtil.getValue(config.getMqttThreads(), "AUKLET_THREADS_MQTT", "auklet.threads.mqtt", LOUD_SECURITY_EXCEPTIONS);
        int mqttThreadsFromConfig = mqttThreadsFromConfigMaybeNull == null ? 3 : mqttThreadsFromConfigMaybeNull;
        if (mqttThreadsFromConfig < 1) mqttThreadsFromConfig = 3;
        this.mqttThreads = mqttThreadsFromConfig;

        // Finalizing the config dir may cause changes to the filesystem, so we wait to do this
        // until we've validated the rest of the config, in case there is a config error; this
        // approach avoids unnecessary filesystem changes for bad configs.
        LOGGER.debug("Determining which config directory to use.");
        if (androidContext == null) {
            this.platform = new JavaPlatform();
        } else {
            this.platform = new AndroidPlatform(androidContext);
        }
        this.configDir = platform.obtainConfigDir(SysUtil.getValue(config.getConfigDir(), "AUKLET_CONFIG_DIR", "auklet.config.dir", LOUD_SECURITY_EXCEPTIONS));
        if (configDir == null) throw new AukletException("Could not find or create any config directory; see previous logged errors for details.");

        LOGGER.debug("Configuring agent resources.");
        this.https = new Https(config.getSslCertificates());
        this.deviceAuth = new DeviceAuth();

        LOGGER.debug("Getting IP/MAC address.");
        this.macHash = Util.getMacAddressHash();
        String ip = "";
        try (Response response = this.https.doRequest(new Request.Builder().url("https://checkip.amazonaws.com"))) {
            String responseString = response.body().string().trim();
            if (response.isSuccessful()) {
                ip = responseString;
            } else {
                LOGGER.warn("Could not get public IP address: {}", responseString);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not get public IP address.", e);
        }
        this.ipAddress = ip;

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
            } catch (SecurityException e) {
                if (LOUD_SECURITY_EXCEPTIONS) throw new AukletException("Could not add JVM shutdown hook.", e);
                else throw new AukletException("Could not add JVM shutdown hook: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new AukletException("Could not add JVM shutdown hook.", e);
            }
        } else {
            this.shutdownHook = null;
        }

        if (uncaughtExceptionHandler) {
            try {
                Thread.setDefaultUncaughtExceptionHandler(new AukletExceptionHandler());
            } catch (SecurityException e) {
                if (LOUD_SECURITY_EXCEPTIONS) throw new AukletException("Could not set default uncaught exception handler.", e);
                else throw new AukletException("Could not set default uncaught exception handler: " + e.getMessage());
            }
        }
    }

    /**
     * <p>The Auklet object cannot be cloned.</p>
     *
     * @return nothing.
     * @throws CloneNotSupportedException unconditionally.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException { // NOSONAR
        throw new CloneNotSupportedException();
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
                    // Do not log cancelled tasks during shutdown.
                    DAEMON.logCancelExceptions(false);
                    agent.doShutdown(false);
                    agent = null;
                    DAEMON.logCancelExceptions(true);
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
     * <p>Makes an authenticated request to the Auklet API.</p>
     *
     * @param request a partially built OkHttp request object. This method fully assembles
     * the URL component of the request and also handles authentication.
     * @param path the URL path - that is, the entire URL minus the protocol and host/domain.
     * Must not be {@code null} or empty.
     * @return never {@code null}.
     * @throws AukletException if an error occurs with the request.
     */
    @NonNull public Response doApiRequest(@NonNull Request.Builder request, @NonNull String path) throws AukletException {
        if (request == null) throw new AukletException("HTTP request is null.");
        if (Util.isNullOrEmpty(path)) throw new AukletException("URL path is null or empty.");
        request
                .url(this.baseUrl + Util.addLeadingSlash(path))
                .header("Authorization", "JWT " + this.apiKey);
        return this.https.doRequest(request);
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
        if (!viaJvmHook && this.shutdownHook != null) Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        this.sink.shutdown();
        this.https.shutdown();
    }

}
