package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.commons.util.thread.Poller;
import com.rackspace.papi.commons.util.thread.RecurringTask;
import com.rackspace.papi.service.filterchain.SweepingGarbageCollector;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.threading.ThreadingService;
import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

@Component("filterChainGCServiceContext")
public class FilterChainGCServiceContext implements ServiceContext<GarbageCollectionService> {

   public static final String SERVICE_NAME = "powerapi:/services/filter-chain-gc";
   private final GarbageCollectionService filterChainGarbageCollector;
   private DestroyableThreadWrapper gcThread;

   @Autowired
   public FilterChainGCServiceContext(@Qualifier("garbageService") GarbageCollectionService filterChainGarbageCollector) {
      this.filterChainGarbageCollector = filterChainGarbageCollector;
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
      final ThreadingService threadingService = ServletContextHelper.getInstance().getPowerApiContext(sce.getServletContext()).threadingService();
      final Poller poller = new Poller(new RecurringTask() {

         @Override
         public void run() {
            filterChainGarbageCollector.sweepGarbageCollectors();
         }
      }, 2500);

      gcThread = new DestroyableThreadWrapper(threadingService.newThread(poller, "Filter Chain Garbage Collector"), poller);
      gcThread.start();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      gcThread.destroy();
   }
}
