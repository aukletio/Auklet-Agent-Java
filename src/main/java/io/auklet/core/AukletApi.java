package io.auklet.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.AukletException;
import io.auklet.misc.Util;
import net.jcip.annotations.Immutable;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>All HTTP requests to the Auklet API are handled by this class.</p>
 *
 * <p>Configuring the SLF4J logger for this class only controls logging statements for internal
 * operations (e.g. shutting down the HTTP client). To configure logging of HTTP requests/responses,
 * you must configure the SLF4J logger named {@code io.auklet.http}. Levels for this logger are handled
 * as follows:</p>
 *
 * <ul>
 *     <li>Set the logger level to {@code TRACE} to log req/resp lines/headers/bodies.</li>
 *     <li>Set the logger level to {@code DEBUG} to log req/resp lines/headers.</li>
 *     <li>Set the logger level to {@code INFO} to log req/resp lines.</li>
 *     <li>Any other logger level disables HTTP logging.</li>
 * </ul>
 *
 * <p>Due to a technical limitation, all messages logged to {@code io.auklet.http} will always be
 * logged at level {@code INFO} (unless logging is disabled), taking into consideration the behavior
 * of the levels described above.</p>
 */
@Immutable
public final class AukletApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(AukletApi.class);
    private static final Logger HTTP_LOGGER = LoggerFactory.getLogger("io.auklet.http");
    private static final HttpLoggingInterceptor INTERCEPTOR = AukletApi.createLogger();
    private final String apiKey;
    private final OkHttpClient httpClient;

    /**
     * <p>Constructor.</p>
     *
     * @param apiKey the Auklet API key. Never {@code null} or empty.
     * @throws AukletException if the API key is {@code null} or empty.
     */
    public AukletApi(@NonNull String apiKey) throws AukletException {
        if (Util.isNullOrEmpty(apiKey)) throw new AukletException("API key is null or empty.");
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .addInterceptor(AukletApi.INTERCEPTOR)
                .build();
    }

    /**
     * <p>Makes an authenticated request to the Auklet API.</p>
     *
     * @param request never {@code null}.
     * @return never {@code null}.
     * @throws AukletException if an error occurs with the request.
     */
    @NonNull public Response doRequest(@NonNull Request.Builder request) throws AukletException {
        if (request == null) throw new AukletException("HTTP request is null.");
        // We handle auth in this method so that the API key does not have
        // to be shared across classes.
        request.header("Authorization", "JWT " + this.apiKey);
        Request req = request.build();
        try {
            synchronized (this.httpClient) {
                return this.httpClient.newCall(req).execute();
            }
        } catch (IOException e) {
            throw new AukletException("Error while making HTTP request.", e);
        }
    }

    /** <p>Shuts down the internal HTTP client.</p> */
    public void shutdown() {
        synchronized (this.httpClient) {
            if (this.httpClient != null) {
                try {
                    this.httpClient.dispatcher().executorService().shutdown();
                    this.httpClient.connectionPool().evictAll();
                    Cache cache = this.httpClient.cache();
                    if (cache != null) cache.close();
                } catch (IOException e) {
                    LOGGER.warn("Error while shutting down Auklet API.", e);
                }
            }
        }
    }

    /**
     * <p>Creates an OkHttp logging interceptor that sends all HTTP logs to the SLF4J logger named
     * {@code io.auklet.http}.</p>
     *
     * @return never {@code null}.
     */
    @NonNull private static HttpLoggingInterceptor createLogger() {
        HttpLoggingInterceptor.Level level;
        if (HTTP_LOGGER.isTraceEnabled()) level = HttpLoggingInterceptor.Level.BODY;
        else if (HTTP_LOGGER.isDebugEnabled()) level = HttpLoggingInterceptor.Level.HEADERS;
        else if (HTTP_LOGGER.isInfoEnabled()) level = HttpLoggingInterceptor.Level.BASIC;
        else level = HttpLoggingInterceptor.Level.NONE;
        if (level != HttpLoggingInterceptor.Level.NONE) LOGGER.info("Auklet HTTP request logging is enabled.");
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override public void log(String message) {
                HTTP_LOGGER.info(message);
            }
        });
        logging.setLevel(level);
        return logging;
    }

}
