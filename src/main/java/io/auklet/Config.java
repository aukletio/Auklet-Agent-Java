package io.auklet;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.misc.Util;
import net.jcip.annotations.NotThreadSafe;

/**
 * <p>Config object for the {@link Auklet} agent. For fluency, all setter methods in this class return
 * {@code this} to support chaining.</p>
 *
 * <p>The table below shows all the available configuration options, the order of preference when a
 * configuration option is set in multiple ways, and the default value for non-required options. For
 * configuration options that have setter methods, passing {@code null} to those methods will cause
 * the agent to fallback on the environment variables/JVM system properties defined below.</p>
 *
 * <table>
 *   <caption>Auklet Java Agent configuration settings</caption>
 *   <thead>
 *     <tr>
 *       <td>Config element</td>
 *       <td>Setter method</td>
 *       <td>Config value selection order</td>
 *       <td>Default value</td>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>Auto-start</td>
 *       <td><i>N/A</i></td>
 *       <td>
 *         <ol>
 *           <li>Environment variable {@code AUKLET_AUTO_START}</li>
 *           <li>JVM system property {@code auklet.auto.start}</li>
 *         </ol>
 *       </td>
 *       <td>{@code false} (auto-start is disabled)</td>
 *     </tr>
 *     <tr>
 *       <td>Application ID</td>
 *       <td>{@link #setAppId(String)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_APP_ID}</li>
 *           <li>JVM system property {@code auklet.app.id}</li>
 *         </ol>
 *       </td>
 *       <td><i>N/A (required)</i></td>
 *     </tr>
 *     <tr>
 *       <td>API key</td>
 *       <td>{@link #setApiKey(String)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_API_KEY}</li>
 *           <li>JVM system property {@code auklet.api.key}</li>
 *         </ol>
 *       </td>
 *       <td><i>N/A (required)</i></td>
 *     </tr>
 *     <tr>
 *       <td>Base URL</td>
 *       <td>{@link #setBaseUrl(String)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_BASE_URL}</li>
 *           <li>JVM system property {@code auklet.base.url}</li>
 *         </ol>
 *       </td>
 *       <td>{@code https://api.auklet.io}</td>
 *     </tr>
 *     <tr>
 *       <td>Config directory</td>
 *       <td>{@link #setConfigDir(String)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_CONFIG_DIR}</li>
 *           <li>JVM system property {@code auklet.config.dir}</li>
 *           <li>JVM system property {@code user.dir}</li>
 *           <li>JVM system property {@code user.home}</li>
 *           <li>JVM system property {@code java.io.tmpdir}</li>
 *         </ol>
 *       </td>
 *       <td><i>N/A (required)</i></td>
 *     </tr>
 *     <tr>
 *       <td>Auto-shutdown (via JVM shutdown hook)</td>
 *       <td>{@link #setAutoShutdown(Boolean)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_AUTO_SHUTDOWN}</li>
 *           <li>JVM system property {@code auklet.auto.shutdown}</li>
 *         </ol>
 *       </td>
 *       <td>{@code true} (auto-shutdown is enabled)</td>
 *     </tr>
 *     <tr>
 *       <td>Default uncaught exception handler</td>
 *       <td>{@link #setUncaughtExceptionHandler(Boolean)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_UNCAUGHT_EXCEPTION_HANDLER}</li>
 *           <li>JVM system property {@code auklet.uncaught.exception.handler}</li>
 *         </ol>
 *       </td>
 *       <td>{@code true} (uncaught exceptions will be sent to Auklet)</td>
 *     </tr>
 *     <tr>
 *       <td>Serial port (instead of sending data to {@code auklet.io}</td>
 *       <td>{@link #setSerialPort(String)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_SERIAL_PORT}</li>
 *           <li>JVM system property {@code auklet.serial.port}</li>
 *         </ol>
 *       </td>
 *       <td>{@code null} (Auklet will not write data to any serial port)</td>
 *     </tr>
 *     <tr>
 *       <td>Number of MQTT threads</td>
 *       <td>{@link #setMqttThreads(Integer)}</td>
 *       <td>
 *         <ol>
 *           <li>Setter method value</li>
 *           <li>Environment variable {@code AUKLET_THREADS_MQTT}</li>
 *           <li>JVM system property {@code auklet.threads.mqtt}</li>
 *         </ol>
 *       </td>
 *       <td>3</td>
 *     </tr>
 *   </tbody>
 * </table>
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
@NotThreadSafe
public final class Config {

    private String appId = null;
    private String apiKey = null;
    private String baseUrl = null;
    private String configDir = null;
    private Boolean autoShutdown = null;
    private Boolean uncaughtExceptionHandler = null;
    private String serialPort = null;
    private Object androidContext = null;
    private Integer mqttThreads = null;

    /**
     * <p>Sets the Auklet agent's app ID.</p>
     *
     * @param appId may be {@code null}. Empty string is coerced to {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setAppId(@Nullable String appId) {
        if (Util.isNullOrEmpty(appId)) appId = null;
        this.appId = appId;
        return this;
    }

    /**
     * <p>Sets the Auklet agent's API key.</p>
     *
     * @param apiKey may be {@code null}. Empty string is coerced to {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setApiKey(@Nullable String apiKey) {
        if (Util.isNullOrEmpty(apiKey)) apiKey = null;
        this.apiKey = apiKey;
        return this;
    }

    /**
     * <p>Sets the base URL of the Auklet API.</p>
     *
     * @param baseUrl may be {@code null}. Empty string is coerced to {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setBaseUrl(@Nullable String baseUrl) {
        if (Util.isNullOrEmpty(baseUrl)) baseUrl = null;
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * <p>Sets the directory the Auklet agent will use to store its configuration files.</p>
     *
     * @param configDir may be {@code null}. Empty string is coerced to {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setConfigDir(@Nullable String configDir) {
        if (Util.isNullOrEmpty(configDir)) configDir = null;
        this.configDir = configDir;
        return this;
    }

    /**
     * <p>Tells the Auklet agent whether or not to setup a JVM shutdown hook to shut itself down.</p>
     *
     * @param autoShutdown may be {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setAutoShutdown(@Nullable Boolean autoShutdown) {
        this.autoShutdown = autoShutdown;
        return this;
    }

    /**
     * <p>Tells the Auklet agent whether or not to setup a JVM-wide uncaught exception handler.</p>
     *
     * @param uncaughtExceptionHandler may be {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setUncaughtExceptionHandler(@Nullable Boolean uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    /**
     * <p>Tells the Auklet agent to write data to a serial port, instead of to {@code auklet.io}.</p>
     *
     * @param serialPort may be {@code null}. Empty string is coerced to {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setSerialPort(@Nullable String serialPort) {
        if (Util.isNullOrEmpty(serialPort)) serialPort = null;
        this.serialPort = serialPort;
        return this;
    }

    /**
     * <p>Passes the Android context to the Auklet agent, which informs the agent that it is running
     * in an Android app. This object must be an instance of {@code android.content.Context}, or the
     * agent will throw an exception during initialization.</p>
     *
     * @param androidContext the Android context object. If {@code null}, the agent is not running
     * in an Android app.
     * @return {@code this}.
     */
    @NonNull public Config setAndroidContext(@Nullable Object androidContext) {
        this.androidContext = androidContext;
        return this;
    }

    /**
     * <p>Tells the Auklet agent how many MQTT threads to use.</p>
     *
     * @param mqttThreads may be {@code null}. Values less than 1 are coerced to {@code null}.
     * @return {@code this}.
     */
    @NonNull public Config setMqttThreads(@Nullable Integer mqttThreads) {
        if (mqttThreads != null && mqttThreads < 1) mqttThreads = null;
        this.mqttThreads = mqttThreads;
        return this;
    }

    /** <p>Returns the desired app ID.</p> */
    /*package*/ @CheckForNull String getAppId() {
        return appId;
    }

    /** <p>Returns the desired API key.</p> */
    /*package*/ @CheckForNull String getApiKey() {
        return apiKey;
    }

    /** <p>Returns the desired API base URL.</p> */
    /*package*/ @CheckForNull String getBaseUrl() {
        return baseUrl;
    }

    /** <p>Returns the desired config directory.</p> */
    /*package*/ @CheckForNull String getConfigDir() {
        return configDir;
    }

    /** <p>Returns the desired auto-shutdown behavior.</p> */
    /*package*/ @CheckForNull Boolean getAutoShutdown() {
        return autoShutdown;
    }

    /** <p>Returns the desired uncaught exception handler behavior.</p> */
    /*package*/ @CheckForNull Boolean getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    /** <p>Returns the desired serial port.</p> */
    /*package*/ @CheckForNull String getSerialPort() {
        return serialPort;
    }

    /** <p>Returns the Android context.</p> */
    /*package*/ @CheckForNull Object getAndroidContext() { return androidContext; }

    /** <p>Returns the desired number of MQTT threads.</p> */
    /*package*/ @CheckForNull Integer getMqttThreads() { return mqttThreads; }

}
