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
package org.openrepose.filters.ratelimiting;

import org.junit.Test;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.filters.ratelimiting.log.LimitLogger;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LimitLoggerTest {

    final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);

    @Test
    public void shouldReturnUsername() {
        final LimitLogger logger = new LimitLogger("some_username", mockedRequest);
        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN)).thenReturn(null);

        final String userId = logger.getSanitizedUserIdentification();

        assertEquals(userId, "some_username");
    }

    @Test
    public void shouldReturnXForwardedFor() {
        final LimitLogger logger = new LimitLogger("some_username", mockedRequest);

        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN)).thenReturn("some_username");
        when(mockedRequest.getHeader(CommonHttpHeader.X_FORWARDED_FOR)).thenReturn("x-forwarded-for-value");

        final String userId = logger.getSanitizedUserIdentification();

        assertEquals(userId, "x-forwarded-for-value");
    }

    @Test
    public void shouldReturnRequestRemoteHost() {
        final LimitLogger logger = new LimitLogger("some_username", mockedRequest);

        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN)).thenReturn("some_username");
        when(mockedRequest.getHeader(CommonHttpHeader.X_FORWARDED_FOR)).thenReturn(null);
        when(mockedRequest.getRemoteHost()).thenReturn("remote-host-value");

        final String userId = logger.getSanitizedUserIdentification();

        assertEquals(userId, "remote-host-value");
    }
}
