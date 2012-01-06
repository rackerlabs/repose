package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fran
 */
public class FilterContextInitializer {

   private static final Logger LOG = LoggerFactory.getLogger(FilterContextInitializer.class);
   private final FilterContextManager filterContextManager;

   public FilterContextInitializer(FilterConfig filterConfig) {
      filterContextManager = new FilterContextManagerImpl(filterConfig);
   }

   public List<FilterContext> buildFilterContexts(ClassLoaderManagerService classLoaderContextManager, PowerProxy powerProxy, int port) {
      final List<FilterContext> filterContexts = new LinkedList<FilterContext>();
      final Host localHost = new SystemModelInterrogator(powerProxy, port).getLocalHost();

      if (localHost != null) {
         for (com.rackspace.papi.model.Filter papiFilter : localHost.getFilters().getFilter()) {
            if (StringUtilities.isBlank(papiFilter.getName())) {
               LOG.error("Filter declaration has a null or empty name value - please check your system model configuration");
               continue;
            }

            if (classLoaderContextManager.hasFilter(papiFilter.getName())) {
               final FilterContext context = getFilterContext(classLoaderContextManager, papiFilter);

               if (context != null) {
                  filterContexts.add(context);
               }
            } else {
               LOG.error("Unable to satisfy requested filter chain - none of the loaded artifacts supply a filter named " + papiFilter.getName());
            }
         }
      } else {
         LOG.error("Unable to identify the local host in the system model - please check your power-proxy.cfg.xml");
      }

      return filterContexts;
   }

   public FilterContext getFilterContext(ClassLoaderManagerService classLoaderContextManager, Filter papiFilter) {
      FilterContext context = null;

      try {
         context = filterContextManager.loadFilterContext(papiFilter.getName(),
                 classLoaderContextManager.getLoadedApplications());
      } catch (Exception e) {
         LOG.info("Problem loading the filter class. Just process the next filter.", e);
      }

      return context;
   }
}
