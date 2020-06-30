package io.auklet;

import android.content.Context;
import io.auklet.config.DeviceAuth;
import io.auklet.core.DataUsageMonitor;
import io.auklet.platform.JavaPlatform;
import mjson.Json;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TestingTools {

    /*JSON Config testing declaration*/
    protected Json jsonConfig = Json.object()
            .set("organization", "organization_value")
            .set("client_id", "client_id_value")
            .set("id", "id_value")
            .set("client_password", "client_password_value");

    /*JSON Data Limits testing declaration*/
    private Json jsonFeatures = Json.object()
            .set("performance_metrics", false)
            .set("user_metrics", false);

    private Json jsonStorage = Json.object()
            .set("storage_limit", 100);

    private Json jsonData = Json.object()
            .set("cellular_data_limit", 100)
            .set("normalized_cell_plan_date", 100);

    private Json jsonDataLimitsConfig = Json.object()
            .set("features", jsonFeatures)
            .set("emission_period", 100)
            .set("storage", jsonStorage)
            .set("data", jsonData);

    protected Json jsonDataLimits = Json.object()
            .set("config", jsonDataLimitsConfig);

    protected Json newJsonDataLimits = Json.object()
            .set("config", jsonDataLimitsConfig);

    /*JSON Brokers testing declaration*/
    protected Json jsonBrokers = Json.object()
            .set("brokers", "0.0.0.0")
            .set("port", "0000");

    protected Auklet aukletConstructor() throws AukletException, IOException, URISyntaxException {
        Auklet mocked = mock(Auklet.class);
        given(mocked.getAppId()).willReturn("0123456789101112");
        given(mocked.getDeviceAuth()).willReturn(new DeviceAuth());
        given(mocked.getUsageMonitor()).willReturn(new DataUsageMonitor());
        given(mocked.getIpAddress()).willReturn("");
        given(mocked.getPlatform()).willReturn(new JavaPlatform());
        given(mocked.getConfigDir()).willReturn(new File(".auklet").getAbsoluteFile());
        given(mocked.getMqttThreads()).willReturn(2);
        given(mocked.getMacHash()).willReturn("");

        given(mocked.doApiRequest(any(Request.Builder.class), anyString())).willReturn(
                new Response.Builder()
                        .request(new Request.Builder().url("https://api.auklet.io").method("get", null).header("Authorization", "JWT " + "123").build())
                        .addHeader("Authorization", "JWT " + "123")
                        .body(ResponseBody.create(MediaType.parse("application/json"),
                                new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("response.json").toURI())))))
                        .code(201)
                        .protocol(Protocol.HTTP_2)
                        .message("This worked")
                        .build());
        return mocked;
    }

    protected Context getTestContext() {
        Context mcontext = mock(Context.class);
        given(mcontext.getFilesDir()).willReturn(new File("dir/"));
        return mcontext;
    }

    protected void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}