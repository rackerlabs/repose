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

import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.HttpMethod;
import org.openrepose.core.services.ratelimit.config.TimeUnit;

import java.util.LinkedList;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jhopper
 */
public class CachedRateLimitTest {
    private ConfiguredRatelimit cfg;

    @Before
    public void setup() {
        final LinkedList<HttpMethod> methods = new LinkedList<HttpMethod>();
        methods.add(HttpMethod.GET);
        methods.add(HttpMethod.POST);

        cfg = mock(ConfiguredRatelimit.class);

        when(cfg.getId()).thenReturn("12345-ABCDE");
        when(cfg.getUriRegex()).thenReturn(".*");
        when(cfg.getHttpMethods()).thenReturn(methods);
        when(cfg.getValue()).thenReturn(6);
        when(cfg.getUnit()).thenReturn(TimeUnit.MINUTE);
    }

    @Test
    public void shouldGiveAccurateExpirationDates() {
        final CachedRateLimit limit = new CachedRateLimit(cfg, 2);

        long currentTime = System.currentTimeMillis();

        final long soonestRequest = limit.getSoonestRequestTime();
        final long nextExpiration = limit.getNextExpirationTime();

        assertThat(soonestRequest, lessThanOrEqualTo(nextExpiration));
        assertThat(soonestRequest, greaterThanOrEqualTo(currentTime));
        assertThat(nextExpiration, greaterThanOrEqualTo(currentTime));
    }

    @Test
    public void getConfigLimitKey_shouldCreateCorrectCLKey() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);

        assertThat(limit.getConfigId(), equalTo(cfg.getId()));
    }

    @Test
    public void timestamp_get() {
        long before = System.currentTimeMillis();
        final CachedRateLimit limit = new CachedRateLimit(cfg);
        long after = System.currentTimeMillis();

        assertThat(limit.timestamp(), greaterThanOrEqualTo(before));
        assertThat(limit.timestamp(), lessThanOrEqualTo(after));
    }

    @Test
    public void amount_get() {
        final CachedRateLimit limit = new CachedRateLimit(cfg, 1);

        assertThat(limit.amount(), equalTo(1));
    }

    @Test
    public void unit_get() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);

        assertThat(limit.unit(), equalTo(java.util.concurrent.TimeUnit.MINUTES.toMillis(1)));
    }

    @Test
    public void maxAmount_get() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);

        assertThat(limit.maxAmount(), equalTo(6));
    }
}
