package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("ehCacheDatastore")
@ManagedResource(objectName = "com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastore", description = "EHCacheDatastore MBean.")
public class EHCacheDatastore implements Datastore, EHCacheDatastoreMBean {

   private final Cache ehCacheInstance;

    @Autowired
   public EHCacheDatastore(@Qualifier("ehCacheInstance") Cache ehCacheInstance) {
      this.ehCacheInstance = ehCacheInstance;
   }

    @ManagedOperation
    public void removeAllCachedData() {
        ehCacheInstance.removeAll();
    }

   @Override
   public StoredElement get(String key) throws DatastoreOperationException {
      final Element element = ehCacheInstance.get(key);

      if (element != null) {
         return new StoredElementImpl(key, (byte[]) element.getValue());
      }

      return new StoredElementImpl(key, null);
   }

   @Override
   public boolean remove(String key) throws DatastoreOperationException {
      return ehCacheInstance.remove(key);
   }

   @Override
   public void put(String key, byte[] value) throws DatastoreOperationException {
      ehCacheInstance.put(new Element(key, value));
   }

   @Override
   public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
      final Element putMe = new Element(key, value);
      putMe.setTimeToLive((int) TimeUnit.SECONDS.convert(ttl, timeUnit));

      ehCacheInstance.put(putMe);
   }

    @Override
    public boolean remove(String key, boolean notify) throws DatastoreOperationException {
        return remove(key);
    }

    @Override
    public void put(String key, byte[] value, boolean notify) throws DatastoreOperationException {
        put(key, value);
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit, boolean notify) throws DatastoreOperationException {
        put(key, value, ttl, timeUnit);
    }
}
