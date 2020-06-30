package io.auklet.core;

import io.auklet.AukletException;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class AukletApiTest {
    @Test void testAukletApi() throws AukletException {
        try {
            new AukletApi("");
        } catch(AukletException e) {
            assertEquals("io.auklet.AukletException: API key is null or empty.", e.toString());
        }
        //TODO: Create test
        new AukletApi("0");
    }

    @Test void testDoRequest() throws AukletException {
        try {
            AukletApi aukletApi = new AukletApi("");
            aukletApi.doRequest(null);
        } catch (AukletException e) {
            assertEquals("io.auklet.AukletException: API key is null or empty.", e.toString());
        }

        AukletApi aukletApi = new AukletApi("0");
        assertEquals("Response{protocol=h2, code=200, message=, url=https://www.google.com/}",
                     aukletApi.doRequest(new Request.Builder().url("https://google.com")).toString());

        try {
            aukletApi.doRequest(new Request.Builder().url("https://google"));
        } catch (AukletException e) {
            assertEquals("io.auklet.AukletException: Error while making HTTP request.", e.toString());
        }
    }
}