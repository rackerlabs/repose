package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.DeploymentConfiguration;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;
import com.rackspace.papi.service.datastore.impl.redundant.RedundantDatastore;
import com.rackspace.papi.service.datastore.impl.redundant.impl.RedundantCacheDatastoreManager;
import com.rackspace.papi.service.datastore.impl.redundant.impl.RedundantDatastoreImpl;

import javax.servlet.ServletContextEvent;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("datastoreServiceContext")
public class DatastoreServiceContext implements ServiceContext<DatastoreService> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DatastoreServiceContext.class);
    public static final String DATASTORE_NAME = "powerapi:/datastore";
    public static final String SERVICE_NAME = "powerapi:/datastore/service";
    private final DatastoreService datastoreService;
    private final ServiceRegistry registry;
    private ServicePorts ports = null;
    private SystemModel systemModel = null;
    private Configuration defaultConfiguration;
    private CacheManager ehCacheManager;
    private final ConfigurationService configurationManager;
    private final ContainerConfigurationListener configurationListener;
    private final SystemModelUpdateListener systemModelListener;
    private RedundantCacheDatastoreManager redundantDatastoreManager;

    @Autowired
    public DatastoreServiceContext(
            @Qualifier("datastoreService") DatastoreService datastoreService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("configurationManager") ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;
        this.datastoreService = datastoreService;
        this.registry = registry;
        this.configurationListener = new ContainerConfigurationListener();
        this.systemModelListener = new SystemModelUpdateListener();
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public DatastoreService getService() {
        return datastoreService;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    private void stopDatastore() {
        if (redundantDatastoreManager != null) {
            RedundantDatastore datastore = (RedundantDatastoreImpl) redundantDatastoreManager.getDatastore();
            datastore.leaveGroup();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Destroying datastore service context");
        stopDatastore();
        ehCacheManager.removalAll();
        ehCacheManager.shutdown();
        configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
        configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
        /*
         * final Context namingContext = ServletContextHelper.getInstance().namingContext(sce.getServletContext());
         *
         * try { namingContext.destroySubcontext(SERVICE_NAME); } catch (NamingException ne) { LOG.warn("Failure in
         * attempting to destroy sub-context \"" + SERVICE_NAME + "\" - Reason: " + ne.getMessage(), ne); }
         *
         */
    }

    private int getPort(Node node) {
        if (node.getHttpPort() > 0) {
            return node.getHttpPort();
        }

        return node.getHttpsPort();
    }

    private synchronized void createDistributedDatastore() {
        if (systemModel == null || ports == null) {
            return;
        }
        
        /*

        if (!ports.isEmpty()) {
            SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
            ReposeCluster serviceDomain = interrogator.getLocalServiceDomain(systemModel);
            Node localHost = interrogator.getLocalHost(systemModel);

            final Set<Subscriber> subscribers = new HashSet<Subscriber>();
            for (Node node : serviceDomain.getNodes().getNode()) {
                subscribers.add(new Subscriber(node.getHostname(), -1, getPort(node)));
            }

            if (redundantDatastoreManager == null) {
                redundantDatastoreManager = new RedundantCacheDatastoreManager(ehCacheManager, subscribers, "*", localHost.getHostname(), getPort(localHost));
                datastoreService.registerDatastoreManager("redundantDatastore", redundantDatastoreManager);
            } else {
                RedundantDatastore datastore = (RedundantDatastoreImpl) redundantDatastoreManager.getDatastore();
                datastore.addSubscribers(subscribers);
            }
        }
        */
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
            ServicePorts ports = new ServicePorts();

            if (deployConfig != null) {
                if (deployConfig.getHttpPort() != null) {
                    ports.add(new Port("http", deployConfig.getHttpPort()));
                }

                if (deployConfig.getHttpsPort() != null) {
                    ports.add(new Port("https", deployConfig.getHttpsPort()));
                }
            }

            return ports;
        }

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();
            ports = determinePorts(deployConfig);
            createDistributedDatastore();
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Init our local default cache and a new service object to hold it
        defaultConfiguration = new Configuration();
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        ehCacheManager = CacheManager.create(defaultConfiguration);

        datastoreService.registerDatastoreManager(DatastoreService.DEFAULT_LOCAL, new EHCacheDatastoreManager(ehCacheManager));
        configurationManager.subscribeTo("container.cfg.xml", configurationListener, ContainerConfiguration.class);
        configurationManager.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);

        register();
    }
}
