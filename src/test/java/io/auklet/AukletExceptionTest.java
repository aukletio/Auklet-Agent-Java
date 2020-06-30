package io.auklet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AukletExceptionTest {
    AukletException aukletException;

    @Test void TestEmptyAukletException() {
        aukletException = new AukletException();
        try {
            throw aukletException;
        } catch (AukletException e) {
            assertEquals("io.auklet.AukletException", e.toString());
        }
    }

    @Test void TestStringAukletException() {
        aukletException = new AukletException("message");
        try {
            throw aukletException;
        } catch (AukletException e) {
            assertEquals("io.auklet.AukletException: message", e.toString());
        }
    }

    @Test void TestStringThrowableAukletException() {
        Throwable cause = new Throwable();
        aukletException = new AukletException("message", cause);
        try {
            throw aukletException;
        } catch (AukletException e) {
            assertEquals("io.auklet.AukletException: message", e.toString());
        }
    }

    @Test void TestThrowableAukletException() {
        Throwable cause = new Throwable();
        aukletException = new AukletException(cause);
        try {
            throw aukletException;
        } catch (AukletException e) {
            assertEquals("io.auklet.AukletException: java.lang.Throwable", e.toString());
        }
    }
}
