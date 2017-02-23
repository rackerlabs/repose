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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.impl.DatastoreServiceImpl;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class ReposeLocalCacheTest {

    public static class TestParent {

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
            datastoreService = new DatastoreServiceImpl();
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
}
