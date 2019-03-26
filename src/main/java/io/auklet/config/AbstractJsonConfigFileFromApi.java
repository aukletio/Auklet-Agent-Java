package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.AukletException;
import io.auklet.misc.Util;
import mjson.Json;
import net.jcip.annotations.NotThreadSafe;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * <p>Base class of all Auklet agent JSON config files that are sourced from the API.</p>
 */
@NotThreadSafe
public abstract class AbstractJsonConfigFileFromApi extends AbstractConfigFileFromApi<Json> {

    /**
     * <p>Submits a request to the Auklet API and returns the response as a JSON object.</p>
     *
     * @param request the API request. Never {@code null}.
     * @return never {@code null}.
     * @throws AukletException if the request is {@code null}, or if the request fails or has an error.
     */
    @NonNull protected final Json makeJsonRequest(@NonNull Request.Builder request) throws AukletException {
        if (request == null) throw new AukletException("JSON HTTP request is null.");
        try (Response response = this.getAgent().getApi().doRequest(request)) {
            String responseString = response.body().string();
            if (response.isSuccessful()) {
                return Util.validateJson(Util.readJson(responseString), this.getClass().getName());
            } else {
                throw new AukletException(String.format("Error while getting Auklet JSON config file '%s': %s: %s", this.getName(), response.message(), responseString));
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new AukletException(String.format("Error while getting Auklet JSON config file '%s'.", this.getName()), e);
        }
    }

    @Override protected void writeToDisk(@NonNull Json contents) throws AukletException {
        if (contents == null) throw new AukletException("Input is null.");
        try {
            Util.writeUtf8(this.file, contents.toString());
        } catch (IOException e) {
            throw new AukletException("Could not save JSON file to disk.", e);
        }
    }

}
