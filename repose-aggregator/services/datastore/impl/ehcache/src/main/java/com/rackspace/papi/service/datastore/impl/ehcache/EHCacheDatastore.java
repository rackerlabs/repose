package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import net.sf.ehcache.Element;
import net.sf.ehcache.Ehcache;

import java.util.concurrent.TimeUnit;

public class EHCacheDatastore implements Datastore {

   private final Ehcache ehCacheInstance;

   public EHCacheDatastore(Ehcache ehCacheInstance ) {
      this.ehCacheInstance = ehCacheInstance;
      
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
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit, boolean notify)
            throws DatastoreOperationException {
        put(key, value, ttl, timeUnit);
    }

    @Override
    public void removeAllCacheData() {
        ehCacheInstance.removeAll();
    }
}
