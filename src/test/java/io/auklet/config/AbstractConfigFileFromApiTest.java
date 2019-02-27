//TODO: Finish test module
package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.AukletException;
import mjson.Json;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbstractConfigFileFromApiTest {
    AbstractConfigFileFromApi abstractConfigFileFromApi;
    @BeforeAll void setup() {
        abstractConfigFileFromApi = new AbstractJsonConfigFileFromApi() {
            @Override
            protected Json readFromDisk() {
                return null;
            }

            @NonNull
            @Override
            protected Json fetchFromApi() throws AukletException {
                return null;
            }

            @NonNull
            @Override
            protected void writeToDisk(@NonNull Json contents) throws AukletException {

            }

            @NonNull
            @Override
            protected String getName() {
                return null;
            }
        };
    }

    @Test void testLoadConfig() {

    }
}
