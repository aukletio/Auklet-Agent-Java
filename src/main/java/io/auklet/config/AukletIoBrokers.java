package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.AukletException;
import io.auklet.core.Util;
import mjson.Json;
import net.jcip.annotations.NotThreadSafe;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>This file contains MQTT connection information for sending data to {@code auklet.io}.</p>
 */
@NotThreadSafe
public final class AukletIoBrokers extends AbstractJsonConfigFileFromApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(AukletIoBrokers.class);

    private final String url;

    /**
     * <p>Constructor.</p>
     *
     * @throws AukletException if the underlying config file cannot be obtained from the filesystem/API,
     * or if it cannot be written to disk.
     */
    public AukletIoBrokers() throws AukletException {
        LOGGER.debug("Loading auklet.io MQTT broker configuration.");
        Json config = this.loadConfig();
        this.url = "ssl://" + config.at("brokers").asString() + ":" + config.at("port").asString();
    }

    @Override public String getName() {
        return "brokers";
    }

    /**
     * <p>Returns the MQTT connection URL</p>
     *
     * @return never {@code null}.
     */
    @NonNull public String getUrl() { return this.url; }

    @Override protected Json readFromDisk() {
        try {
            String fromDisk = this.getStringFromDisk();
            if (fromDisk.isEmpty()) return null;
            return Util.validateJson(Util.readJson(fromDisk), this.getClass().getName());
        } catch (AukletException | IOException | IllegalArgumentException e) {
            LOGGER.warn("Could not read broker config from disk, will re-download from API.", e);
            return null;
        }
    }

    @Override protected Json fetchFromApi() throws AukletException {
        Request.Builder request = new Request.Builder()
                .url(this.getAgent().getBaseUrl() + "/private/devices/config/").get()
                .header("Content-Type", "application/json; charset=utf-8");
        return this.makeJsonRequest(request);
    }

    @Override protected void writeToDisk(@NonNull Json contents) throws AukletException {
        if (contents == null) throw new AukletException("Input is null.");
        Util.writeUtf8(this.file, contents.toString());
    }

}
