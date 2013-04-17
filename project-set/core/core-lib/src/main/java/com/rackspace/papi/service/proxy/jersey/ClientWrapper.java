package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ClientWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(ClientWrapper.class);
    private static final int SHORT_TERM_CACHE_SIZE = 5000;
    private static final int SHORT_TERM_TTL = 2;
    private static final int LONG_TERM_CACHE_SIZE = 1000;
    private static final int LONG_TERM_TTL = 60;
    private static final String CACHE_NAME_PREFIX = "jersey:resources:";
    private static final String CACHE_MANAGER_NAME = "JerseyCacheManager";
    private final Client client;
    private CacheManager cacheManager;
    private Cache shortCache;
    private Cache longCache;
    private final ReposeInstanceInfo instanceInfo;

    public ClientWrapper(Client client, boolean requestLogging, ReposeInstanceInfo instanceInfo) {
        this.client = client;
        this.instanceInfo = instanceInfo;
        
        if (requestLogging) {
            LOG.warn("Enabling info logging of jersey client requests");
            client.addFilter(new LoggingFilter());
        } else {
            LOG.warn("**** Jersey client request logging not enabled *****");
        }
        initCache();
    }

    @Override
    protected void finalize() throws Throwable {
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
        config.setName((instanceInfo != null? instanceInfo.toString() + ":": "") + CACHE_MANAGER_NAME);
        config.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        config.setUpdateCheck(false);
        cacheManager = CacheManager.newInstance(config);
        shortCache = newCache(cacheManager, SHORT_TERM_CACHE_SIZE, SHORT_TERM_TTL, SHORT_TERM_TTL);
        longCache = newCache(cacheManager, LONG_TERM_CACHE_SIZE, LONG_TERM_TTL, LONG_TERM_TTL);
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
