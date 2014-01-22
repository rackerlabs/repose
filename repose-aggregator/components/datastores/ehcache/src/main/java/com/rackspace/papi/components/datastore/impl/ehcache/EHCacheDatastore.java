package com.rackspace.papi.components.datastore.impl.ehcache;

import com.rackspace.papi.components.datastore.Datastore;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

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
    public void removeAll() {
        ehCacheInstance.removeAll();
    }
}
