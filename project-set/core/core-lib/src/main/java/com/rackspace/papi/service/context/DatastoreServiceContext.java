package com.rackspace.papi.service.context;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.PowerApiDatastoreService;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;
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
         // TODO:Log
      }
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      // Init our local default cache and a new service object to hold it
      final Configuration defaultConfiguration = new Configuration();
      defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
      defaultConfiguration.setUpdateCheck(false);

      final CacheManager ehCacheManager = new CacheManager(defaultConfiguration);

      datastoreService = new PowerApiDatastoreService();
      datastoreService.registerDatastoreManager(DatastoreService.DEFAULT_LOCAL, new EHCacheDatastoreManager(ehCacheManager));
   }
}
