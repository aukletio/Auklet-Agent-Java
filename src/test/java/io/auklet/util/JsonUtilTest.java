package io.auklet.util;

import io.auklet.AukletException;
import io.auklet.config.DataUsageLimit;
import io.auklet.config.DeviceAuth;
import mjson.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {
    @Test void testReadJson() {
        assertNotEquals("{}", JsonUtil.readJson("{1:1}"));
    }

    @Test void testValidateJson() throws AukletException, URISyntaxException, IOException {
        final Json testJson = JsonUtil.readJson(new String(Files.readAllBytes(
                Paths.get(getClass().getClassLoader().getResource("response.json").toURI()))));
        // if no exceptions are thrown then the validation has passed
        assertNotNull(JsonUtil.validateJson(testJson, DeviceAuth.class.getName()));

        AukletException e = assertThrows(
                AukletException.class, new Executable() {
                    @Override
                    public void execute() throws AukletException {
                        JsonUtil.validateJson(null, DeviceAuth.class.getName());
                    }
                });
        assertEquals(e.getMessage(), "Input is null.");

        AukletException e2 = assertThrows(
                AukletException.class, new Executable() {
                    @Override
                    public void execute() throws AukletException {
                        JsonUtil.validateJson(testJson, DataUsageLimit.class.getName());
                    }
                });
        assert(e2.getMessage().contains("Errors while parsing Auklet JSON config file"));
    }
}
