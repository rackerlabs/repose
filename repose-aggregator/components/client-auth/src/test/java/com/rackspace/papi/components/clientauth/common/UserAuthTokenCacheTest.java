package com.rackspace.papi.components.clientauth.common;

import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class UserAuthTokenCacheTest {
   
   public static final String VALID_USER = "user", VALID_AUTH_TOKEN = "abcdef", CACHE_PREFIX = "prefix";

   
   public static class WhenCachingAuthTokens {

      protected AuthTokenCache infoCache;
      protected AuthToken originalUser;
      protected Datastore mockedDatastore;

      @Before
      public void standUp() throws Exception {
         originalUser = mock(AuthToken.class, withSettings().serializable());
         when(originalUser.getUserId()).thenReturn("userId");
         when(originalUser.getUsername()).thenReturn("username");
         when(originalUser.getExpires()).thenReturn(10000l);
         when(originalUser.getRoles()).thenReturn("roles");
         when(originalUser.getTokenId()).thenReturn("token");
         mockedDatastore = mock(Datastore.class);
         
         final String cacheFullName =CACHE_PREFIX + "." + VALID_USER; 
         
         final StoredElement storedElement = new StoredElementImpl(cacheFullName, ObjectSerializer.instance().writeObject(originalUser));
         when(mockedDatastore.get(eq(cacheFullName))).thenReturn(storedElement);
         
         infoCache = new AuthTokenCache(mockedDatastore, "prefix") {

            @Override
            public boolean validateToken(AuthToken cachedValue) {
               return true;
            }
         };
      }

      @Test
      public void shouldCorrectlyRetrieveValidCachedUserInfo() {
         final AuthToken user = infoCache.getUserToken(VALID_USER);

         assertEquals("UserId must match original", originalUser.getUserId(), user.getUserId());
         assertEquals("Username must match original", originalUser.getUsername(), user.getUsername());
         assertEquals("Expires must match original", originalUser.getExpires(), user.getExpires());
         assertEquals("Roles must match original", originalUser.getRoles(), user.getRoles());
         assertEquals("TokenId must match original", originalUser.getTokenId(), user.getTokenId());
      }
   }
}
