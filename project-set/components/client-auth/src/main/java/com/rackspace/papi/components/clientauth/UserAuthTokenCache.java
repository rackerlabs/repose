package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public abstract class UserAuthTokenCache<T extends Serializable> {

   private final Datastore store;
   private final Class<T> elementClass;

   public UserAuthTokenCache(Datastore store, Class<T> elementClass) {
      this.store = store;
      this.elementClass = elementClass;
   }

   private T getElementAsType(StoredElement element) {
      return element == null || element.elementIsNull() ? null : element.elementAs(elementClass);
   }
   
   public T getUserToken(String userId, String token) {
      T candidate = getElementAsType(store.get(getCachePrefix() + "." + userId));
      return validateToken(candidate, token)? candidate: null;
   }
   
   public void storeToken(String userId, T token, int ttl) throws IOException {
      if (userId == null || token == null || ttl < 0) {
         // TODO Should we throw an exception here?
         return;
      }
      store.put(getCachePrefix() + "." + userId, ObjectSerializer.instance().writeObject(token), ttl, TimeUnit.MILLISECONDS);
   }
   
   public abstract String getCachePrefix();
   
   public abstract boolean validateToken(T cachedValue, String passedValue);
}
