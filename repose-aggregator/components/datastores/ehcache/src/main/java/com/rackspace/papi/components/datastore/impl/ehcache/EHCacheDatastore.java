package com.rackspace.papi.components.datastore.impl.ehcache;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.*;
import net.sf.ehcache.Element;
import net.sf.ehcache.Ehcache;
import org.apache.commons.lang3.SerializationUtils;

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
        //todo: switch to time to idle instead of time to live?

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
        Element currentElement = ehCacheInstance.putIfAbsent(element);
        Serializable returnValue;

        if(currentElement == null) {
            returnValue = SerializationUtils.clone(potentialNewValue);
            currentElement = element;
        }
        else {
            returnValue = (Serializable)((Patchable)currentElement.getValue()).applyPatch(patch);
        }

        if(ttl >= 0) {
            int convertedTtl = (int) TimeUnit.SECONDS.convert(ttl, timeUnit);
            if(convertedTtl > currentElement.getTimeToIdle()) {
                currentElement.setTimeToIdle(convertedTtl);

                //todo: should we round up to the nearest second rather than always rounding down by casting?
                int currentLifeSpan = (int)TimeUnit.SECONDS.convert(System.currentTimeMillis() - currentElement.getCreationTime(), TimeUnit.MILLISECONDS);
                currentElement.setTimeToLive(currentLifeSpan + convertedTtl);
            }
        }

        return returnValue;
    }

    @Override
    public void removeAll() {
        ehCacheInstance.removeAll();
    }
}
