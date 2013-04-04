package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.PowerApiDatastoreService;
import net.sf.ehcache.CacheManager;
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
        DatastoreManager datastoreManager;
        CacheManager cacheManager;
        Datastore datastore;

        @Before
        public void setUp() {
            tenantId = "tenantId";
            token = "token";
            userId = "userId";
            reposeLocalCacheMock = mock(ReposeLocalCache.class);
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
            datastoreService = new PowerApiDatastoreService();
            cacheManager = new CacheManager();
            datastoreManager = new EHCacheDatastoreManager(cacheManager);
            datastoreService.registerDatastoreManager(DatastoreService.DEFAULT_LOCAL, datastoreManager);
            reposeLocalCacheReal = new ReposeLocalCache(datastoreService);

            final String key = "my element";
            byte[] value = {1, 2, 3};

            datastore = datastoreService.defaultDatastore().getDatastore();
            datastore.put(key,value);

            assertNotNull(datastoreService.defaultDatastore().getDatastore().get(key).elementBytes());

            reposeLocalCacheReal.removeAllCacheData();

            assertNull(datastoreService.defaultDatastore().getDatastore().get(key).elementBytes());
        }
    }
}
