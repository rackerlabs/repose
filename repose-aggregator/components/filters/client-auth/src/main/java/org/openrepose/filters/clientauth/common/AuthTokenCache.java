package org.openrepose.filters.clientauth.common;

import com.rackspace.auth.AuthToken;
import org.openrepose.services.datastore.Datastore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuthTokenCache implements DeleteableCache{

   private final Datastore store;
   private final String cachePrefix;

   public AuthTokenCache(Datastore store, String cachePrefix) {
      this.store = store;
      this.cachePrefix = cachePrefix;
   }

   public AuthToken getUserToken(String tokenId) {
      AuthToken candidate = (AuthToken)store.get(cachePrefix + "." + tokenId);//Looking into the datastore for this token.
      return validateToken(candidate)? candidate: null;
   }
   
   public void storeToken(String tokenId, AuthToken token, int ttl) throws IOException {
      if (tokenId == null || token == null || ttl < 0) {
         // TODO Should we throw an exception here?
         return;
      }
      
      store.put(cachePrefix + "." + tokenId, token, ttl, TimeUnit.MILLISECONDS);
   }
   
   @Override
   public boolean deleteCacheItem(String userId){
      return store.remove(cachePrefix + "." + userId);
   }

   public boolean validateToken(AuthToken cachedValue) {
      return cachedValue != null && cachedValue.getTokenId() != null && cachedValue.safeTokenTtl() > 0;
   }
}
