package io.auklet.sink;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.TestingTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.TestCase.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbstractSinkTest extends TestingTools {
    private AbstractSink abstractSink;
    @BeforeAll void setup() throws AukletException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Auklet auklet = aukletConstructor(null);
        abstractSink = new AbstractSink() {
            @Override
            protected void write(@NonNull byte[] bytes) throws AukletException {

            }

            @Override
            public void start(@NonNull Auklet agent) throws AukletException {
                agent.getDeviceAuth().start(agent);
                setAgent(agent);
            }
        };
        abstractSink.start(auklet);
    }

    @Test void testSend() throws AukletException {
        abstractSink.send(new AukletException());
        assertNotNull(abstractSink.msgpack.toByteArray());
    }
}