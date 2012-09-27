package com.rackspace.papi.components.clientauth.common;

import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuthTokenCache {

   private final Datastore store;
   private final String cachePrefix;

   public AuthTokenCache(Datastore store, String cachePrefix) {
      this.store = store;
      this.cachePrefix = cachePrefix;
   }

   private AuthToken getElementAsType(StoredElement element) {
      return element == null || element.elementIsNull() ? null : element.elementAs(AuthToken.class);
   }
   
   public AuthToken getUserToken(String userId, String token) {
      AuthToken candidate = getElementAsType(store.get(cachePrefix + "." + userId));
      return validateToken(candidate, token)? candidate: null;
   }
   
   public void storeToken(String userId, AuthToken token, int ttl) throws IOException {
      if (userId == null || token == null || ttl < 0) {
         // TODO Should we throw an exception here?
         return;
      }
      
      byte[] data = ObjectSerializer.instance().writeObject(token);
      
      store.put(cachePrefix + "." + userId, data, ttl, TimeUnit.MILLISECONDS);
   }

   public boolean validateToken(AuthToken cachedValue, String passedValue) {
      return cachedValue != null && cachedValue.getTokenId() != null && cachedValue.safeTokenTtl() > 0 && cachedValue.getTokenId().equals(passedValue);
   }
}
