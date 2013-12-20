package org.openrepose.components.rackspace.authz.cache;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.StoredElementImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
         when(mockedDatastore.get(eq(CACHED_TOKEN_FULLNAME))).thenReturn(new StoredElementImpl(CACHED_TOKEN, ObjectSerializer.instance().writeObject(endpointList)));
         when(mockedDatastore.get(eq(NON_CACHED_TOKEN_FULLNAME))).thenReturn(new StoredElementImpl(NON_CACHED_TOKEN, null));

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
         
         verify(mockedDatastore, times(1)).put(eq(NON_CACHED_TOKEN_FULLNAME), any(byte[].class), anyInt(), eq(TimeUnit.SECONDS));
      }
   }
}
