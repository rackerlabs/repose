package com.rackspace.papi.service.datastore.distributed.impl.ehcache;

import org.openrepose.services.datastore.api.Datastore;
import org.openrepose.services.datastore.api.DatastoreService;
import org.openrepose.services.datastore.impl.DatastoreServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
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
            datastore.put(key,value);

            assertNotNull(datastoreService.getDefaultDatastore().get(key));

            reposeLocalCacheReal.removeAllCacheData();

            assertNull(datastoreService.getDefaultDatastore().get(key));
        }
    }
}
