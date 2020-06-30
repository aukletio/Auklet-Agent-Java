package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbstractConfigFileTest {
    AbstractConfigFile abstractConfigFile;
    @BeforeAll void setup() {
        abstractConfigFile = new AbstractConfigFile() {
            @NonNull
            @Override
            protected String getName() {
                return null;
            }
        };
    }

    @Test void testGetName() {

    }
}
