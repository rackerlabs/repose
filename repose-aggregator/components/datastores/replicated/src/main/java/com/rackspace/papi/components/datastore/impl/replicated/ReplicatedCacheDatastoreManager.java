package com.rackspace.papi.components.datastore.impl.replicated;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreManager;
import com.rackspace.papi.components.datastore.impl.replicated.data.Subscriber;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;

public class ReplicatedCacheDatastoreManager implements DatastoreManager {

    public static final String REPLICATED_DISTRIBUTED = "distributed/replicated";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ReplicatedCacheDatastoreManager.class);
    private static final String CACHE_NAME_PREFIX = "PAPI_REDUNDANT_";
    private final CacheManager cacheManagerInstance;
    private final String cacheName;
    private final Set<Subscriber> subscribers;
    private final String address;
    private final int port;
    private ReplicatedDatastoreImpl datastore;
    private int maxQueueSize;

    public ReplicatedCacheDatastoreManager(CacheManager cacheManagerInstance, Set<Subscriber> subscribers, String address, int port, int maxQueueSize) {
        this.cacheManagerInstance = cacheManagerInstance;

        cacheName = CACHE_NAME_PREFIX + UUID.randomUUID().toString();
        this.subscribers = subscribers;
        this.address = address;
        this.port = port;
        this.maxQueueSize = maxQueueSize;

        init();
    }

    private void init() {
        final Cache cache = new Cache(cacheName, 20000, false, false, 5, 2);
        cacheManagerInstance.addCache(cache);
    }

    @Override
    public void destroy() {
        cacheManagerInstance.removeCache(cacheName);
    }

    public void setMaxQueueSize(int maxQueueSize) {
        synchronized (this) {
            if (datastore == null) {
                this.maxQueueSize = maxQueueSize;
            } else {
                LOG.warn("Datastore has been created.  Changing max queue size is not possible");
            }
        }
    }

    public void updateSubscribers(Set<Subscriber> subscribers) {
        this.subscribers.addAll(subscribers);
        synchronized (this) {
            if (datastore != null) {
                datastore.addSubscribers(subscribers);
            }
        }
    }

    @Override
    public Datastore getDatastore() {
        try {
            synchronized (this) {
                if (datastore == null) {
                    datastore = new ReplicatedDatastoreImpl(subscribers, address, port, cacheManagerInstance.getCache(cacheName), maxQueueSize);
                    datastore.joinGroup();
                }
                return datastore;
            }
        } catch (UnknownHostException ex) {
            LOG.error("Error creating redundant datastore", ex);
        } catch (IOException ex) {
            LOG.error("Error creating redundant datastore", ex);
        }

        return null;
    }

    @Override
    public boolean isDistributed() {
        return true;
    }
}
