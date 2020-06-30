package io.auklet.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasAgentTest {
    HasAgent hasAgent;
    @BeforeAll void setup() {
        hasAgent = new HasAgent() {
            @Override
            public void start(@NonNull Auklet agent) throws AukletException {

            }
        };
    }

    @Test void testSetAgent() {

    }
}
