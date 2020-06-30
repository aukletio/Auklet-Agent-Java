package io.auklet;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.misc.Util;
import mjson.Json;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class TestingTools {
    private Cipher aesCipher;
    private Key aesKey;


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

    protected Auklet aukletConstructor(Config config) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, RuntimeException {
        if (config == null) {
            config = new Config().setAppId("0123456789101112")
                    .setApiKey("123");
        }

        Constructor<Auklet> aukletConstructor = Auklet.class.getDeclaredConstructor(config.getClass());
        aukletConstructor.setAccessible(true);
        return aukletConstructor.newInstance(config);
    }
}