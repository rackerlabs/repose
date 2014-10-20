package org.openrepose.core.filter;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.systemmodel.Filter;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.services.classloader.ClassLoaderManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.FilterConfig;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fran
 *
 * Takes info from config file to initialize a filter context.
 */
public class FilterContextInitializer {

   private static final Logger LOG = LoggerFactory.getLogger(FilterContextInitializer.class);
   private final FilterContextManager filterContextManager;

   public FilterContextInitializer(FilterConfig filterConfig) {
      filterContextManager = new FilterContextManagerImpl(filterConfig);
  }

   public List<FilterContext> buildFilterContexts(ClassLoaderManagerService classLoaderContextManager, ReposeCluster domain, Node localHost) {
       //TODO: it wants to set the thread name based on instance info
      final List<FilterContext> filterContexts = new LinkedList<FilterContext>();

      if (localHost == null || domain == null) {
         LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
         throw new IllegalArgumentException("Domain and host cannot be null");
      }

       if (domain.getFilters() != null && domain.getFilters().getFilter() != null) {
           for (org.openrepose.core.systemmodel.Filter papiFilter : domain.getFilters().getFilter()) {

               if (StringUtilities.isBlank(papiFilter.getName())) {
                   LOG.error(
                           "Filter declaration has a null or empty name value - please check your system model configuration");
                   continue;
               }

               if (classLoaderContextManager.hasFilter(papiFilter.getName())) {
                   final FilterContext context = getFilterContext(classLoaderContextManager, papiFilter);

                   if (context != null) {
                       filterContexts.add(context);
                   } else {
                       filterContexts.add(new FilterContext(null, null, papiFilter));
                   }
               } else {
                   LOG.error(
                           "Unable to satisfy requested filter chain - none of the loaded artifacts supply a filter named " +
                                   papiFilter.getName());
                   filterContexts.add(new FilterContext(null, null, papiFilter));
               }
           }
       }

       return filterContexts;
   }

   public FilterContext getFilterContext(ClassLoaderManagerService classLoaderContextManager, Filter papiFilter) {
      FilterContext context = null;

      try {
         context = filterContextManager.loadFilterContext(
                 papiFilter,
                 classLoaderContextManager.getLoadedApplications());
      } catch (Exception e) {
         LOG.info("Problem loading the filter class. Just process the next filter. Reason: {}", e.getMessage(), e);
      }

      return context;
   }
}
