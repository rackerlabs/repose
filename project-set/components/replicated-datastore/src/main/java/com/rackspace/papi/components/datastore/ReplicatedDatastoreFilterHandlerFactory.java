package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.DeploymentConfiguration;
import com.rackspace.papi.domain.Port;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicatedDatastoreFilterHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ReplicatedDatastoreFilterHandler> {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicatedDatastoreFilterHandlerFactory.class);
    private ServicePorts ports = null;
    private SystemModel systemModel = null;
    private ReplicatedCacheDatastoreManager redundantDatastoreManager;
    private final DatastoreService service;
    private final CacheManager ehCacheManager;

    public ReplicatedDatastoreFilterHandlerFactory(DatastoreService service, CacheManager ehCacheManager) {
        this.service = service;
        this.ehCacheManager = ehCacheManager;
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listeners = new HashMap<Class, UpdateListener<?>>();
        listeners.put(SystemModel.class, new SystemModelUpdateListener());
        listeners.put(ContainerConfiguration.class, new ContainerConfigurationListener());

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
        if (redundantDatastoreManager != null) {
            ReplicatedDatastore datastore = (ReplicatedDatastoreImpl) redundantDatastoreManager.getDatastore();
            datastore.leaveGroup();
            LOG.info("Unregistering datastore " + DatastoreService.REDUNDANT_DISTRIBUTED);
            service.unregisterDatastoreManager(DatastoreService.REDUNDANT_DISTRIBUTED);
        }
    }

    private synchronized void createDistributedDatastore() {
        if (systemModel == null || ports == null) {
            return;
        }

        if (!ports.isEmpty()) {
            SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
            ReposeCluster serviceDomain = interrogator.getLocalServiceDomain(systemModel);
            Node localHost = interrogator.getLocalHost(systemModel);

            if (redundantDatastoreManager == null) {
                LOG.info("Registering datastore " + DatastoreService.REDUNDANT_DISTRIBUTED);
                redundantDatastoreManager = new ReplicatedCacheDatastoreManager(ehCacheManager, getDatastoreNodes(serviceDomain), localHost.getHostname(), getPort(localHost));
                service.registerDatastoreManager(DatastoreService.REDUNDANT_DISTRIBUTED, redundantDatastoreManager);
            } else {
                ReplicatedDatastore datastore = (ReplicatedDatastoreImpl) redundantDatastoreManager.getDatastore();
                datastore.addSubscribers(getDatastoreNodes(serviceDomain));
            }
        }
    }

    private class SystemModelUpdateListener implements UpdateListener<SystemModel> {

        @Override
        public void configurationUpdated(SystemModel config) {
            if (config == null) {
                return;
            }

            systemModel = config;
            createDistributedDatastore();
        }
    }

    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private ServicePorts determinePorts(DeploymentConfiguration deployConfig) {
            ServicePorts servicePorts = new ServicePorts();

            if (deployConfig != null) {
                if (deployConfig.getHttpPort() != null) {
                    servicePorts.add(new Port("http", deployConfig.getHttpPort()));
                }

                if (deployConfig.getHttpsPort() != null) {
                    servicePorts.add(new Port("https", deployConfig.getHttpsPort()));
                }
            }

            return servicePorts;
        }

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();
            ports = determinePorts(deployConfig);
            createDistributedDatastore();
        }
    }

    @Override
    protected ReplicatedDatastoreFilterHandler buildHandler() {
        return new ReplicatedDatastoreFilterHandler();
    }
}
