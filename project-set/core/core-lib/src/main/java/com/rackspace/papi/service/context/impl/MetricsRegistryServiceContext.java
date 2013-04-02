
package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.reporting.metrics.MetricsRegistryService;
import com.rackspace.papi.service.reporting.metrics.impl.MetricsRegistryServiceImpl;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("metricsRegistryServiceContext")
public class MetricsRegistryServiceContext implements ServiceContext<MetricsRegistryService>{
   
   private MetricsRegistryService metricsRegistryService;
   private final String serviceName = "MetricsRegistryService";
   private final ServiceRegistry registry;
   
   @Autowired
   public MetricsRegistryServiceContext(@Qualifier("metricsRegistry") MetricsRegistryService metricsRegistryService,
           @Qualifier("serviceRegistry") ServiceRegistry registry){
      
      this.metricsRegistryService = metricsRegistryService;
      this.registry = registry;
   }
   
   private void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

   @Override
   public String getServiceName() {
      return serviceName;
   }

   @Override
   public MetricsRegistryService getService() {
      return metricsRegistryService;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      
      if(metricsRegistryService == null){
         metricsRegistryService = new MetricsRegistryServiceImpl();
      }
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      // Nothing really to destroy
   }
   
}
