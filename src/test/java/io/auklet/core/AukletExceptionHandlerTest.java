package io.auklet.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AukletExceptionHandlerTest {
    AukletExceptionHandler aukletExceptionHandler;
    @BeforeAll void setup() {
        aukletExceptionHandler = new AukletExceptionHandler();
    }

    @Test void testUncaughtException() {

    }
}
