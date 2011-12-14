package com.rackspace.papi.service.filterchain;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.commons.util.thread.Poller;
import com.rackspace.papi.commons.util.thread.RecurringTask;
import com.rackspace.papi.filter.resource.PowerFilterChainGarbageCollector;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.threading.ThreadingService;
import javax.servlet.ServletContextEvent;

public class FilterChainGCServiceContext implements ServiceContext<FilterChainGarbageCollectorService> {

   public static final String SERVICE_NAME = "powerapi:/services/filter-chain-gc";
   private final PowerFilterChainGarbageCollector filterChainGarbageCollector;
   private DestroyableThreadWrapper gcThread;

   public FilterChainGCServiceContext() {
      filterChainGarbageCollector = new PowerFilterChainGarbageCollector();
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public FilterChainGarbageCollectorService getService() {
      return filterChainGarbageCollector;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ThreadingService threadingService = ServletContextHelper.getPowerApiContext(sce.getServletContext()).threadingService();
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
