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
package org.openrepose.core.services.datastore.distributed.impl.ehcache;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.impl.DatastoreServiceImpl;
import org.openrepose.core.services.reporting.metrics.MetricsService;

import java.util.Optional;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ReposeLocalCacheTest {

    MetricsService metricsService = mock(MetricsService.class);
    MetricRegistry metricRegistry = mock(MetricRegistry.class);
    Timer timer = mock(Timer.class);
    Timer.Context timerContext = mock(Timer.Context.class);

    String tenantId, token, userId;
    ReposeLocalCache reposeLocalCacheMock;
    ReposeLocalCache reposeLocalCacheReal;
    DatastoreService datastoreService;
    Datastore datastore;

    @Before
    public void setUp() {
        tenantId = "tenantId";
        token = "token";
        userId = "userId";
        reposeLocalCacheMock = mock(ReposeLocalCache.class);

        reset(metricsService);
        reset(metricRegistry);
        reset(timer);
        reset(timerContext);

        when(metricsService.getRegistry()).thenReturn(metricRegistry);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        when(timer.time()).thenReturn(timerContext);

        datastoreService = new DatastoreServiceImpl(Optional.of(metricsService));
    }

    @Test
    public void shouldReturnBooleanWhenRemovingTokensAndRoles() {
        assertThat(reposeLocalCacheMock.removeTokenAndRoles(tenantId, token), is(instanceOf(Boolean.class)));
    }

    @Test
    public void shouldReturnBooleanWhenRemovingGroups() {
        assertThat(reposeLocalCacheMock.removeGroups(tenantId, token), is(instanceOf(Boolean.class)));
    }

    @Test
    public void shouldReturnBooleanWhenRemovingLimits() {
        assertThat(reposeLocalCacheMock.removeLimits(userId), is(instanceOf(Boolean.class)));
    }

    @Test
    public void shouldRemoveCacheData() {
        reposeLocalCacheReal = new ReposeLocalCache(datastoreService);

        final String key = "my element";
        String value = "1, 2, 3";

        datastore = datastoreService.getDefaultDatastore();
        datastore.put(key, value);

        assertNotNull(datastoreService.getDefaultDatastore().get(key));

        reposeLocalCacheReal.removeAllCacheData();

        assertNull(datastoreService.getDefaultDatastore().get(key));
    }
}
