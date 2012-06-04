package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.util.UUID;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

public class ClientWrapper {

    private static final String CACHE_NAME_PREFIX = "jersey:resources:";
    private final Client client;
    private CacheManager cacheManager;
    private Cache shortCache;
    private Cache longCache;

    public ClientWrapper(Client client) {
        this.client = client;
        initCache();
    }

    @Override
    public void finalize() throws Throwable {
        cacheManager.removalAll();
        cacheManager.shutdown();
        super.finalize();
    }
    
    private Cache newCache(CacheManager manager, int items, int ttl, int idle) {
        String name = CACHE_NAME_PREFIX + UUID.randomUUID().toString();
        Cache cache = new Cache(name, items, false, false, ttl, idle);
        manager.addCache(cache);
        return cache;
    }

    private void initCache() {
        final Configuration config = new Configuration();
        config.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        config.setUpdateCheck(false);
        cacheManager = new CacheManager(config);
        shortCache = newCache(cacheManager, 5000, 2, 2);
        longCache = newCache(cacheManager, 1000, 60, 60);
    }

    public WebResource resource(String url) {
        return resource(url, false);
    }

    private WebResource getCachedValue(String url, Cache cache) {
        Element element = cache.get(url);
        return element == null ? null : (WebResource) element.getObjectValue();

    }

    private void storeValue(String url, WebResource resource, Cache cache) {
        Element element = new Element(url, resource);
        cache.put(element);
    }

    private WebResource getResource(String url, Cache cache) {
        synchronized (cache) {
            WebResource resource = getCachedValue(url, cache);
            if (resource == null) {
                resource = client.resource(url);
                storeValue(url, resource, cache);
            }
            return resource;
        }
    }

    public WebResource resource(String url, boolean cacheable) {
        if (!cacheable) {
            return getResource(url, shortCache);
        }
        
        return getResource(url, longCache);
    }

    public Client getClient() {
        return client;
    }
}
