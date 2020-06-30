package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.AukletException;
import io.auklet.TestingTools;
import io.auklet.misc.Util;
import mjson.Json;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbstractConfigFileFromApiTest extends TestingTools {
    private AbstractConfigFileFromApi abstractConfigFileFromApi;
    @BeforeAll void setup() {

        abstractConfigFileFromApi = new AbstractJsonConfigFileFromApi() {
            @Override
            protected Json readFromDisk() {
                return null;
            }

            @NonNull
            @Override
            protected Json fetchFromApi() throws AukletException {
                return Json.object().set("Data", "test");
            }

            @NonNull
            @Override
            protected void writeToDisk(@NonNull Json contents) throws AukletException {
                try {
                    Util.writeUtf8(this.file, contents.toString());
                } catch (IOException e) {
                    // Purposefully to be empty
                }
            }

            @NonNull
            @Override
            protected String getName() {
                return null;
            }
        };
        abstractConfigFileFromApi.file = new File(".auklet/test");
    }

    @Test void testLoadConfig() throws AukletException {
        abstractConfigFileFromApi.loadConfig();
        assertEquals(true, abstractConfigFileFromApi.file.length() > 0);
        abstractConfigFileFromApi.file.delete();
    }

    @Test void testGetStringFromDisk() throws AukletException, IOException {
        Json jsonData = Json.object().set("Data", "test");
        abstractConfigFileFromApi.writeToDisk(jsonData);
        assertEquals(jsonData.toString(), abstractConfigFileFromApi.getStringFromDisk());
    }
}
