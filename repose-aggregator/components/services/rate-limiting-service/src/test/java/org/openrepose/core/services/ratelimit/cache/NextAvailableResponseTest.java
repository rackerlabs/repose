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
package org.openrepose.core.services.ratelimit.cache;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NextAvailableResponseTest {
    private NextAvailableResponse nextAvailableResponse;
    private Pair<ConfiguredRatelimit, CachedRateLimit> limitPair;
    private long expirationTime;

    @Before
    public void setUp() throws Exception {
        ConfiguredRatelimit configLimit = mock(ConfiguredRatelimit.class);
        CachedRateLimit cachedLimit = mock(CachedRateLimit.class);

        expirationTime = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)).getTime();

        when(cachedLimit.amount()).thenReturn(5);
        when(cachedLimit.maxAmount()).thenReturn(10);
        when(cachedLimit.getNextExpirationTime()).thenReturn(expirationTime);

        limitPair = Pair.of(configLimit, cachedLimit);
        nextAvailableResponse = new NextAvailableResponse(limitPair);
    }

    @Test
    public void testGetResetTime() throws Exception {
        assertThat(nextAvailableResponse.getResetTime().getTime(), equalTo(expirationTime));
    }

    @Test
    public void testHasRequestsRemaining() throws Exception {
        assertTrue(nextAvailableResponse.hasRequestsRemaining());
    }

    @Test
    public void testGetCurrentLimitAmount() throws Exception {
        assertThat(nextAvailableResponse.getCurrentLimitAmount(), equalTo(5));
    }

    @Test
    public void testGetLimitPair() throws Exception {
        assertThat(nextAvailableResponse.getLimitPair(), equalTo(limitPair));
    }
}
