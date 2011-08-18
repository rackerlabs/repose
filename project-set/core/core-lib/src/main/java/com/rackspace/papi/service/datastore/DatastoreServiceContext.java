package com.rackspace.papi.service.datastore;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.datastore.impl.PowerApiDatastoreService;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;
import com.rackspace.papi.servlet.ServletContextInitException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;

public class DatastoreServiceContext implements ServiceContext<DatastoreService> {

    public static final String DATASTORE_NAME = "powerapi:/datastore";
    public static final String SERVICE_NAME = "powerapi:/datastore/service";
    
    private DatastoreService datastoreService;
    
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
        final Context namingContext = ServletContextHelper.namingContext(sce.getServletContext());

        try {
            namingContext.destroySubcontext(SERVICE_NAME);
        } catch (NamingException ne) {
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final Context namingContext = ServletContextHelper.namingContext(sce.getServletContext());

        final Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));

        final CacheManager ehCacheManager = new CacheManager(defaultConfiguration);

        try {
            final DatastoreNamingContext datastoreNamingContext = new DatastoreNamingContext("default", namingContext.getEnvironment(), new EHCacheDatastoreManager(ehCacheManager));
            namingContext.bind(DATASTORE_NAME + "/local/default", datastoreNamingContext);
        } catch (NamingException ne) {
            throw new ServletContextInitException(ne.getExplanation(), ne);
        }
        
        datastoreService = new PowerApiDatastoreService(namingContext, DATASTORE_NAME);
    }
}
