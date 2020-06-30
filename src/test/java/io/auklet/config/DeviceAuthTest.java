package io.auklet.config;

import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.TestingTools;
import mjson.Json;
import okhttp3.Request.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeviceAuthTest extends TestingTools {
    private Auklet auklet;
    private Json jsonConfig;
    private DeviceAuth deviceAuth;

    @BeforeAll void setup() throws AukletException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, RuntimeException {
        jsonConfig = Json.object().set("organization", "organization_value")
                                  .set("client_id", "client_id_value")
                                  .set("id", "id_value")
                                  .set("client_password", "client_password_value");

        auklet = aukletConstructor(null);

        deviceAuth = Mockito.spy(DeviceAuth.class);
        Mockito.doReturn(jsonConfig).when(deviceAuth).loadConfig();
        Mockito.doReturn(jsonConfig).when(deviceAuth).makeJsonRequest(any(Builder.class));

        deviceAuth.start(auklet);
    }

    @Test void testGetName() {
        assertEquals("AukletAuth", deviceAuth.getName());
    }

    @Test void testGetOrganizationId() {
        assertEquals("organization_value", deviceAuth.getOrganizationId());
    }

    @Test void testGetClientId() {
        assertEquals("client_id_value", deviceAuth.getClientId());
    }

    @Test void testGetClientUsername() {
        assertEquals("id_value", deviceAuth.getClientUsername());
    }

    @Test void testGetClientPassword() {
        assertEquals("client_password_value", deviceAuth.getClientPassword());
    }

    @Test void testGetMqttEventsTopic() {
        assertEquals("java/events/organization_value/id_value", deviceAuth.getMqttEventsTopic());
    }

    @Test void testReadWriteFromToDisk() throws AukletException {
        deviceAuth.writeToDisk(jsonConfig);
        assertEquals(jsonConfig, deviceAuth.readFromDisk());
    }

    @Test void testFetchFromApi() throws AukletException {
        assertEquals(jsonConfig, deviceAuth.fetchFromApi());
    }
}
