package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public abstract class UserAuthGroupsCache<T extends Serializable> {

    private final Datastore store;
   private final Class<T> elementClass;

   public UserAuthGroupsCache(Datastore store, Class<T> elementClass) {
      this.store = store;
      this.elementClass = elementClass;
   }

   private T getElementAsType(StoredElement element) {
      return element == null || element.elementIsNull() ? null : element.elementAs(elementClass);
   }

   public T getUserGroup(String userId) {
      T candidate = getElementAsType(store.get(getCachePrefix() + "." + userId));

      return validateGroup(candidate) ? candidate: null;
   }

   public void storeGroups(String userId, T groups, int ttl) throws IOException {
      if (userId == null || groups == null || ttl < 0) {
         // TODO Should we throw an exception here?
         return;
      }

      byte[] data = ObjectSerializer.instance().writeObject(groups);

      store.put(getCachePrefix() + "." + userId, data, ttl, TimeUnit.MILLISECONDS);
   }

   public abstract String getCachePrefix();

   public abstract boolean validateGroup(T cachedValue);
}
