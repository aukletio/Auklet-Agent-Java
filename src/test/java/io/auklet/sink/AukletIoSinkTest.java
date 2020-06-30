package io.auklet.sink;

import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.TestingTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AukletIoSinkTest extends TestingTools {
    private AukletIoSink aukletIoSink;
    private Auklet auklet;

    @BeforeAll void setup() throws AukletException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, RuntimeException {
        auklet = aukletConstructor(null);
        aukletIoSink = new AukletIoSink();

        auklet.getDeviceAuth().start(auklet);
        auklet.getUsageMonitor().start(auklet);
    }

    @Test void testStart() {
        try {
            aukletIoSink.start(auklet);
        } catch (AukletException e) {
            assertEquals(false, e.toString().contains("Could not initialize MQTT sink."));
        }
    }
}
