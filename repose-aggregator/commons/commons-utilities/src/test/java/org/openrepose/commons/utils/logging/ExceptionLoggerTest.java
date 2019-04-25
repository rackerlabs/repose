/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.logging;

import org.junit.Before;
import org.junit.Test;
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
public class ExceptionLoggerTest {
    private static final String TEST_MESSAGE = "test message";

    private Logger loggerRef;
    private ExceptionLogger exceptionLogger;
    private Throwable cause;

    @Before
    public void setup() {
        loggerRef = mock(Logger.class);
        exceptionLogger = new ExceptionLogger(loggerRef);
        cause = new IllegalStateException();
    }

    @Test
    public void shouldUseMessage() {
        RuntimeException actual =
                exceptionLogger.newException(TEST_MESSAGE, RuntimeException.class);

        assertEquals(TEST_MESSAGE, actual.getMessage());
    }

    @Test
    public void shouldLeaveCauseNull() {
        RuntimeException actual =
                exceptionLogger.newException(TEST_MESSAGE, RuntimeException.class);

        assertNull(actual.getCause());
    }

    @Test
    public void whenCreatingNewExceptionsWithoutCauseShouldLogError() {
        exceptionLogger.newException(TEST_MESSAGE, RuntimeException.class);

        Throwable t = null;

        verify(loggerRef).error(TEST_MESSAGE, t);
    }

    @Test
    public void shouldSetCause() {
        RuntimeException actual =
                new ExceptionLogger(loggerRef).newException(
                        TEST_MESSAGE, cause, RuntimeException.class);

        assertEquals(cause, actual.getCause());
    }

    @Test
    public void whenCreatingNewExceptionsWithCauseShouldLogError() {
        new ExceptionLogger(loggerRef)
                .newException(TEST_MESSAGE, cause, RuntimeException.class);

        verify(loggerRef).error(TEST_MESSAGE, cause);
    }
}
