/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.filters.authz.cache;

import org.openrepose.core.services.datastore.Datastore;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class EndpointListCacheImplTest {

   public static final String NON_CACHED_TOKEN = "not-cached",
           NON_CACHED_TOKEN_FULLNAME = EndpointListCacheImpl.getCacheNameForToken(NON_CACHED_TOKEN),
           CACHED_TOKEN = "cached",
           CACHED_TOKEN_FULLNAME = EndpointListCacheImpl.getCacheNameForToken(CACHED_TOKEN),
           PUBLIC_URL = "http://f.com",
           REGION = "ORD",
           NAME = "Nova",
           TYPE = "compute";


   public static class WhenGettingCachedEndpointLists {

      protected EndpointListCacheImpl cache;
      protected Datastore mockedDatastore;

      @Before
      public void standUp() throws Exception {
         final LinkedList<CachedEndpoint> endpointList = new LinkedList<CachedEndpoint>();
         endpointList.add(new CachedEndpoint(PUBLIC_URL, REGION, NAME, TYPE));

         mockedDatastore = mock(Datastore.class);
         when(mockedDatastore.get(eq(CACHED_TOKEN_FULLNAME))).thenReturn(endpointList);

         cache = new EndpointListCacheImpl(mockedDatastore, 300);
      }

      @Test
      public void shouldReturnNullForEndpointListsNotInCache() {
         final List<CachedEndpoint> endpointList = cache.getCachedEndpointsForToken(NON_CACHED_TOKEN);
         
         verify(mockedDatastore, times(1)).get(eq(NON_CACHED_TOKEN_FULLNAME));
         assertTrue("Should return null for endpoint lists not yet in cache", endpointList == null);
      }

      @Test
      public void shouldGetCachedEnpointLists() throws Exception {
         final List<CachedEndpoint> endpointList = cache.getCachedEndpointsForToken(CACHED_TOKEN);

         assertNotNull("Cached endpoint list must not be null", endpointList);
         assertFalse("Cached endpoint list must not be empty", endpointList.isEmpty());

         final CachedEndpoint onlyEndpoint = endpointList.get(0);

         assertEquals("Cache must return valid endpoints", PUBLIC_URL, onlyEndpoint.getPublicUrl());
      }
      
      @Test
      public void shouldCacheEndpointList() throws Exception {
         final LinkedList<CachedEndpoint> endpointList = new LinkedList<CachedEndpoint>();
         endpointList.add(new CachedEndpoint(PUBLIC_URL, REGION, NAME, TYPE));
         
         cache.cacheEndpointsForToken(NON_CACHED_TOKEN, endpointList);
         
         verify(mockedDatastore, times(1)).put(eq(NON_CACHED_TOKEN_FULLNAME), any(Serializable.class), anyInt(), eq(TimeUnit.SECONDS));
      }
   }
}
