package com.rackspace.papi.commons.util.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/25/11
 * Time: 12:52 PM
 */
@RunWith(Enclosed.class)
public class ExceptionLoggerTest {
    private static final String TEST_MESSAGE = "test message";

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public static class WhenCreatingNewExceptionsWithoutCause {
        private Logger loggerRef;
        private ExceptionLogger exceptionLogger;

        @Before
        public void setup() {
            loggerRef = mock(Logger.class);
            exceptionLogger = new ExceptionLogger(loggerRef);
        }

        @Test
        public void shouldUseMessage() {
            RuntimeException actual =
                    exceptionLogger.newException(TEST_MESSAGE,  RuntimeException.class);

            assertEquals(TEST_MESSAGE, actual.getMessage());
        }

        @Test
        public void shouldLeaveCauseNull() {
            RuntimeException actual =
                    exceptionLogger.newException(TEST_MESSAGE,  RuntimeException.class);

            assertNull(actual.getCause());
        }

        @Test
        public void shouldLogError() {
            exceptionLogger.newException(TEST_MESSAGE,  RuntimeException.class);

            Throwable t = null;

            verify(loggerRef).error(TEST_MESSAGE, t);
        }
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public static class WhenCreatingNewExceptionsWithCause {
        private Logger loggerRef;
        private Throwable cause;

        @Before
        public void setup() {
            loggerRef = mock(Logger.class);
            cause = new IllegalStateException();
        }

        @Test
        public void shouldSetCause() {
            RuntimeException actual =
                    new ExceptionLogger(loggerRef).newException(
                            TEST_MESSAGE,  cause, RuntimeException.class);

            assertEquals(cause, actual.getCause());
        }

        @Test
        public void shouldLogError() {
            new ExceptionLogger(loggerRef)
                    .newException(TEST_MESSAGE,  cause, RuntimeException.class);

            verify(loggerRef).error(TEST_MESSAGE, cause);
        }
    }
}
