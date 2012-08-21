package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.jmx.ReposeMBeanObjectNames;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.ehcache.ReposeEHCache;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;

import javax.management.*;
import javax.servlet.ServletContextEvent;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

@Component("datastoreServiceContext")
public class DatastoreServiceContext implements ServiceContext<DatastoreService> {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreServiceContext.class);

    public static final String DATASTORE_NAME = "powerapi:/datastore";
    public static final String SERVICE_NAME = "powerapi:/datastore/service";
    private final DatastoreService datastoreService;
    private final ServiceRegistry registry;

    @Autowired
    public DatastoreServiceContext(
            @Qualifier("datastoreSerivce") DatastoreService datastoreService,
            @Qualifier("serviceRegistry") ServiceRegistry registry) {
        this.datastoreService = datastoreService;
        this.registry = registry;
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
        /*
         * final Context namingContext = ServletContextHelper.getInstance().namingContext(sce.getServletContext());
         *
         * try { namingContext.destroySubcontext(SERVICE_NAME); } catch (NamingException ne) { LOG.warn("Failure in
         * attempting to destroy sub-context \"" + SERVICE_NAME + "\" - Reason: " + ne.getMessage(), ne); }
         *
         */
        unregisterEHCacheMBean();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Init our local default cache and a new service object to hold it
        final Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        final CacheManager ehCacheManager = new CacheManager(defaultConfiguration);

        datastoreService.registerDatastoreManager(DatastoreService.DEFAULT_LOCAL, new EHCacheDatastoreManager(ehCacheManager));

        registerEHCacheMBean();

        register();
    }

    // TODO: Create an MBean Service that manages the registration and unregistration of all system MBeans
    private void registerEHCacheMBean() {
        ReposeEHCache cache = new ReposeEHCache(datastoreService.defaultDatastore().getDatastore());
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            final ObjectName reposeEHCacheObjectName = new ReposeMBeanObjectNames().getReposeEHCache();
            LOG.info("Registering MBean " + reposeEHCacheObjectName.getCanonicalName());

            platformMBeanServer.registerMBean(cache, reposeEHCacheObjectName);
        } catch (NotCompliantMBeanException e) {
            LOG.warn("MBean registration failed.  Cli-Util \"remove-token\" and \"remove-groups\" may not function: " + e.getMessage());
        } catch (InstanceAlreadyExistsException e) {
            LOG.info("MBean already exists " + e.getMessage());
        } catch (MBeanRegistrationException e) {
            LOG.warn("MBean registration failed.  Cli-Util \"remove-token\" and \"remove-groups\" may not function: " + e.getMessage());
        } catch (Exception e) {
            LOG.warn("MBean registration failed.  Cli-Util \"remove-token\" and \"remove-groups\" may not function: " + e.getMessage());
        }
    }

    private void unregisterEHCacheMBean() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName reposeEHCacheObjectName = new ReposeMBeanObjectNames().getReposeEHCache();
        LOG.info("Unregistering MBean " + reposeEHCacheObjectName.getCanonicalName());
        try {
            platformMBeanServer.unregisterMBean(reposeEHCacheObjectName);
        } catch (InstanceNotFoundException e) {
            LOG.info("MBean instance not found: " + reposeEHCacheObjectName.getCanonicalName());
        } catch (MBeanRegistrationException e) {
            LOG.info("MBean unregistration failed: " + reposeEHCacheObjectName.getCanonicalName());
        }
    }
}
