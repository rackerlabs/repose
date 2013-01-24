package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.replicated.ReplicatedDatastore;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import com.rackspace.papi.service.datastore.impl.replicated.impl.ReplicatedCacheDatastoreManager;
import com.rackspace.papi.service.datastore.impl.replicated.impl.ReplicatedDatastoreImpl;
import java.util.*;
import net.sf.ehcache.CacheManager;
import org.openrepose.components.datastore.replicated.config.ReplicatedDatastoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicatedDatastoreFilterHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ReplicatedDatastoreFilterHandler> {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicatedDatastoreFilterHandlerFactory.class);
    private ServicePorts ports = null;
    private SystemModel systemModel = null;
    private ReplicatedCacheDatastoreManager replicatedDatastoreManager;
    private final DatastoreService service;
    private final CacheManager ehCacheManager;
    private ReplicatedDatastoreConfiguration configuration;
    private final Object lock = new Object();

    public ReplicatedDatastoreFilterHandlerFactory(DatastoreService service, CacheManager ehCacheManager, ServicePorts ports) {
        this.service = service;
        this.ehCacheManager = ehCacheManager;
        this.ports = ports;
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listeners = new HashMap<Class, UpdateListener<?>>();
        listeners.put(SystemModel.class, new SystemModelUpdateListener());
        listeners.put(ReplicatedDatastoreConfiguration.class, new ConfigListener());

        return listeners;
    }

    private int getPort(Node node) {
        if (node.getHttpPort() > 0) {
            return node.getHttpPort();
        }

        return node.getHttpsPort();
    }

    private Set<Subscriber> getDatastoreNodes(ReposeCluster serviceDomain) {
        final Set<Subscriber> subscribers = new HashSet<Subscriber>();
        for (Node node : serviceDomain.getNodes().getNode()) {
            subscribers.add(new Subscriber(node.getHostname(), -1, getPort(node)));
        }


        return subscribers;
    }

    public void stopDatastore() {
        synchronized (lock) {
            if (replicatedDatastoreManager != null) {
                ReplicatedDatastore datastore = (ReplicatedDatastoreImpl) replicatedDatastoreManager.getDatastore();
                datastore.leaveGroup();
                LOG.info("Unregistering datastore " + ReplicatedCacheDatastoreManager.REPLICATED_DISTRIBUTED);
                service.unregisterDatastoreManager(ReplicatedCacheDatastoreManager.REPLICATED_DISTRIBUTED);
            }
        }
    }

    private boolean createDistributedDatastore() {
        synchronized (lock) {
            if (systemModel == null || ports == null) {
                return false;
            }

            if (!ports.isEmpty()) {
                SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
                ReposeCluster serviceDomain = interrogator.getLocalServiceDomain(systemModel);
                Node localHost = interrogator.getLocalHost(systemModel);

                int maxQueueSize = 0;
                if (configuration != null) {
                    maxQueueSize = configuration.getQueueSizeLimit();
                }

                if (replicatedDatastoreManager == null) {
                    LOG.info("Registering datastore " + ReplicatedCacheDatastoreManager.REPLICATED_DISTRIBUTED);
                    replicatedDatastoreManager = new ReplicatedCacheDatastoreManager(ehCacheManager, getDatastoreNodes(serviceDomain), localHost.getHostname(), getPort(localHost), maxQueueSize);
                    service.registerDatastoreManager(ReplicatedCacheDatastoreManager.REPLICATED_DISTRIBUTED, replicatedDatastoreManager);
                } else {
                    replicatedDatastoreManager.setMaxQueueSize(maxQueueSize);
                    replicatedDatastoreManager.updateSubscribers(getDatastoreNodes(serviceDomain));
                }
                return true;
            } else {
                LOG.warn("Service Ports are not available");
            }
            
            return false;
        }
    }

    private class SystemModelUpdateListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel config) {
            if (config == null) {
                return;
            }

            synchronized (lock) {
                systemModel = config;
            }
            isInitialized = createDistributedDatastore();
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    private class ConfigListener implements UpdateListener<ReplicatedDatastoreConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ReplicatedDatastoreConfiguration config) {
            synchronized (lock) {
                configuration = config;
            }
            isInitialized = createDistributedDatastore();
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected ReplicatedDatastoreFilterHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }
        return new ReplicatedDatastoreFilterHandler();
    }
}
