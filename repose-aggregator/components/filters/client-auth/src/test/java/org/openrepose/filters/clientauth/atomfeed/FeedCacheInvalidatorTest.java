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
package org.openrepose.filters.clientauth.atomfeed;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.mockito.Mockito.*;

public class FeedCacheInvalidatorTest {
    FeedCacheInvalidator fci;

    @Before
    public void setUp() {
        /*
        * TODO: figure out how to mock singleton
        * fci.run() not invoked, but need to mock fci for checking if while loop is done
        */
        fci = mock(FeedCacheInvalidator.class);
    }

    @Test
    public void shouldSaveTraceIDInMDC() {
        when(fci.isDone()).thenReturn(false).thenReturn(true);
        fci.run();
        assert (MDC.get("X-Trans-Id") != null);
        verify(fci, times(2)).isDone();
    }
}
