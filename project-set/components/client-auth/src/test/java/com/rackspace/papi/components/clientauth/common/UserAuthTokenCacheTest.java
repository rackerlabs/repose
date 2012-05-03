package com.rackspace.papi.components.clientauth.common;

import com.rackspace.papi.components.clientauth.common.UserAuthTokenCache;
import com.rackspace.auth.openstack.ids.CachableUserInfo;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class UserAuthTokenCacheTest {
   
   public static final String VALID_USER = "user", VALID_AUTH_TOKEN = "abcdef", CACHE_PREFIX = "prefix";

   
   public static class WhenCachingAuthTokens {

      protected UserAuthTokenCache<CachableUserInfo> userInfoCache;
      protected CachableUserInfo originalUserInfo;
      protected Datastore mockedDatastore;

      @Before
      public void standUp() throws Exception {
         originalUserInfo = new CachableUserInfo("token", "userId", "username", "roles", 10000, null);
         mockedDatastore = mock(Datastore.class);
         
         final String cacheFullName =CACHE_PREFIX + "." + VALID_USER; 
         
         final StoredElement storedElement = new StoredElementImpl(cacheFullName, ObjectSerializer.instance().writeObject(originalUserInfo));
         when(mockedDatastore.get(eq(cacheFullName))).thenReturn(storedElement);
         
         userInfoCache = new UserAuthTokenCache<CachableUserInfo>(mockedDatastore, CachableUserInfo.class) {

            @Override
            public String getCachePrefix() {
               return "prefix";
            }

            @Override
            public boolean validateToken(CachableUserInfo cachedValue, String passedValue) {
               return true;
            }
         };
      }

      @Test
      public void shouldCorrectlyRetrieveValidCachedUserInfo() {
         final CachableUserInfo userInfo = userInfoCache.getUserToken(VALID_USER, VALID_AUTH_TOKEN);
         
         assertEquals("UserId must match original", originalUserInfo.getUserId(), userInfo.getUserId());
         assertEquals("Username must match original", originalUserInfo.getUsername(), userInfo.getUsername());
         assertEquals("Expires must match original", originalUserInfo.getExpires(), userInfo.getExpires());
         assertEquals("Roles must match original", originalUserInfo.getRoles(), userInfo.getRoles());
         assertEquals("TokenId must match original", originalUserInfo.getTokenId(), userInfo.getTokenId());
      }
   }
}
