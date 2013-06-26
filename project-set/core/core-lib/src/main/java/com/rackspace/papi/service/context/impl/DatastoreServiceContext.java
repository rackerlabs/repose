package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;

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
    private static final String CACHE_MANAGER_NAME = "LocalDatastoreCacheManager";
    private final DatastoreService datastoreService;
    private final ServiceRegistry registry;
    private CacheManager ehCacheManager;
    private ReposeInstanceInfo instanceInfo;
    @Autowired
    public DatastoreServiceContext(
            @Qualifier("datastoreService") DatastoreService datastoreService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("reposeInstanceInfo") ReposeInstanceInfo instanceInfo) {
        this.datastoreService = datastoreService;
        this.registry = registry;
        this.instanceInfo = instanceInfo;
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

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Destroying datastore service context");
        ehCacheManager.removalAll();
        ehCacheManager.shutdown();
        datastoreService.unregisterDatastoreManager(DatastoreService.DEFAULT_LOCAL);
     }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Init our local default cache and a new service object to hold it
        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setName(instanceInfo.toString() + ":" + CACHE_MANAGER_NAME);
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        ehCacheManager = CacheManager.newInstance(defaultConfiguration);

        datastoreService.registerDatastoreManager(DatastoreService.DEFAULT_LOCAL, new EHCacheDatastoreManager(ehCacheManager));

        register();
    }
}
