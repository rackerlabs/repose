package com.rackspace.papi.components.clientauth.common;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;
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
   
   private Set<String> getElementAsType(StoredElement element) {
      return element == null || element.elementIsNull() ? null : element.elementAs(Set.class);
   }
   
   public void storeUserTokenList(String userId, Set<String> tokens, int ttl) throws IOException {
      if (userId == null || tokens == null || ttl < 0) {
         // TODO Should we throw an exception here?
         return;
      }
      
      byte[] data = ObjectSerializer.instance().writeObject((Serializable)tokens);
      
      store.put(cachePrefix + "." + userId, data, ttl, TimeUnit.MILLISECONDS);
   }
   
   @Override
   public boolean deleteCacheItem(String userId){
      return store.remove(cachePrefix + userId);
   }
   
   public Set<String> getUserTokenList(String userId) {
      Set<String> candidate = getElementAsType(store.get(cachePrefix + "." + userId));
      return candidate;
   }
}
