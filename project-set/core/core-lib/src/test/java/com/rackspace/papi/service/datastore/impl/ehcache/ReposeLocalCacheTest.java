package com.rackspace.papi.service.datastore.impl.ehcache;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class ReposeLocalCacheTest {

    public static class TestParent {

        String tenantId, token, userId;
        ReposeLocalCache reposeLocalCache;

        @Before
        public void setUp() {
            tenantId = "tenantId";
            token = "token";
            userId = "userId";
            reposeLocalCache = mock(ReposeLocalCache.class);
        }

        @Test
        public void shouldReturnBooleanWhenRemovingTokensAndRoles() {
            assertThat(reposeLocalCache.removeTokenAndRoles(tenantId, token), is(instanceOf(Boolean.class)));
        }

        @Test
        public void shouldReturnBooleanWhenRemovingGroups() {
            assertThat(reposeLocalCache.removeGroups(tenantId, token), is(instanceOf(Boolean.class)));
        }

        @Test
        public void shouldReturnBooleanWhenRemovingLimits() {
            assertThat(reposeLocalCache.removeLimits(userId), is(instanceOf(Boolean.class)));
        }
    }
}
