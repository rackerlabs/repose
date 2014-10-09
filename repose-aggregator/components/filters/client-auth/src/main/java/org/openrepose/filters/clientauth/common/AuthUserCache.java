package org.openrepose.filters.clientauth.common;

import org.openrepose.services.datastore.api.Datastore;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Cache for Auth user. 
 */
public class AuthUserCache implements DeleteableCache {
   
   private final Datastore store;
   private final String cachePrefix;
   
   public AuthUserCache(Datastore store, String cachePrefix) {
      this.store = store;
      this.cachePrefix = cachePrefix;
   }
   
   public void storeUserTokenList(String userId, Set<String> tokens, int ttl) throws IOException {
      if (userId == null || tokens == null || ttl < 0) {
         // TODO Should we throw an exception here?
         return;
      }

      store.put(cachePrefix + "." + userId, (Serializable)tokens, ttl, TimeUnit.MILLISECONDS);
   }
   
   @Override
   public boolean deleteCacheItem(String userId){
      return store.remove(cachePrefix + userId);
   }
   
   public Set<String> getUserTokenList(String userId) {
      Set<String> candidate = (Set<String>)store.get(cachePrefix + "." + userId);
      return candidate;
   }
}
