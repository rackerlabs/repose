package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.commons.util.thread.Poller;
import com.rackspace.papi.commons.util.thread.RecurringTask;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.threading.ThreadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("filterChainGCServiceContext")
public class FilterChainGCServiceContext implements ServiceContext<GarbageCollectionService> {

   public static final String SERVICE_NAME = "powerapi:/services/filter-chain-gc";
   private final GarbageCollectionService filterChainGarbageCollector;
   private DestroyableThreadWrapper gcThread;
   private final ServiceRegistry registry;
   private final ThreadingService threadingService;

   @Autowired
   public FilterChainGCServiceContext(
           @Qualifier("garbageService") GarbageCollectionService filterChainGarbageCollector,
           @Qualifier("serviceRegistry") ServiceRegistry registry,
           @Qualifier("threadingServiceContext") ThreadingService threadingService) {
      this.filterChainGarbageCollector = filterChainGarbageCollector;
      this.registry = registry;
      this.threadingService = threadingService;
   }

   public void register() {
      if (registry != null) {
         registry.addService(this);
      }
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public GarbageCollectionService getService() {
      return filterChainGarbageCollector;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final Poller poller = new Poller(new RecurringTask() {

         @Override
         public void run() {
            filterChainGarbageCollector.sweepGarbageCollectors();
         }
      }, 2500);

      gcThread = new DestroyableThreadWrapper(threadingService.newThread(poller, "Filter Chain Garbage Collector"), poller);
      gcThread.start();
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      gcThread.destroy();
   }
}
