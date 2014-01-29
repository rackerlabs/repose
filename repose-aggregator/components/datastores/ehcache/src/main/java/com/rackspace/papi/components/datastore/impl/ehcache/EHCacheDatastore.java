package com.rackspace.papi.components.datastore.impl.ehcache;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.*;
import net.sf.ehcache.Element;
import net.sf.ehcache.Ehcache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class EHCacheDatastore implements Datastore {

    private final Ehcache ehCacheInstance;
    private final static String NAME = "local/default";

    public EHCacheDatastore(Ehcache ehCacheInstance) {
        this.ehCacheInstance = ehCacheInstance;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean remove(String key) {
        return ehCacheInstance.remove(key);
    }

    @Override
    public Serializable get(String key) {
        Element element = ehCacheInstance.get(key);
        if(element != null) {
            return element.getValue();
        }
        else {
            return null;
        }
    }

    @Override
    public void put(String key, Serializable value) {
        ehCacheInstance.put(new Element(key, value));
    }

    @Override
    public void put(String key, Serializable value, int ttl, TimeUnit timeUnit) {
        Element putMe = new Element(key, value);
        putMe.setTimeToLive((int) TimeUnit.SECONDS.convert(ttl, timeUnit));

        ehCacheInstance.put(putMe);
    }

    @Override
    public Serializable patch(String key, Patch patch) throws DatastoreOperationException {
        return patch(key, patch, -1, TimeUnit.MINUTES);
    }

    @Override
    public Serializable patch(String key, Patch patch, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        Serializable potentialNewValue = (Serializable)patch.newFromPatch();
        Element element = new Element(key, potentialNewValue);
        if(ttl != -1) {
            element.setTimeToLive((int)TimeUnit.SECONDS.convert(ttl, timeUnit));
        }
        Element currentElement = ehCacheInstance.putIfAbsent(element);
        if(currentElement == null) {
            return potentialNewValue; //todo: crap, i think this needs to be copied. otherwise we have the potential for multiple changes to be reflected. the copy is taken care of in the patch for all other cases
        }
        else {
            currentElement = ehCacheInstance.get(key);
            return (Serializable)((Patchable)currentElement.getValue()).applyPatch(patch);
        }
    }

    @Override
    public void removeAll() {
        ehCacheInstance.removeAll();
    }
}
