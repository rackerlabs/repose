package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.FilterConfig;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fran
 */
public class FilterContextInitializer {

   private static final Logger LOG = LoggerFactory.getLogger(FilterContextInitializer.class);
   private final FilterContextManager filterContextManager;
   private final ReposeInstanceInfo instanceInfo;

   public FilterContextInitializer(FilterConfig filterConfig, ApplicationContext applicationContext) {
      filterContextManager = new FilterContextManagerImpl(filterConfig, applicationContext);
      instanceInfo = (ReposeInstanceInfo) applicationContext.getBean("reposeInstanceInfo");
      
   }

   public List<FilterContext> buildFilterContexts(ClassLoaderManagerService classLoaderContextManager, ReposeCluster domain, Node localHost) {
      Thread.currentThread().setName(instanceInfo.toString());
      final List<FilterContext> filterContexts = new LinkedList<FilterContext>();

      if (localHost == null || domain == null) {
         LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
         throw new IllegalArgumentException("Domain and host cannot be null");
      }

       if (domain.getFilters() != null && domain.getFilters().getFilter() != null) {
           for (com.rackspace.papi.model.Filter papiFilter : domain.getFilters().getFilter()) {

               //Message to let users know DD filter is deprecated.
               if (!StringUtilities.isBlank(papiFilter.getName()) && papiFilter.getName().equals("dist-datastore")) {
                   LOG.warn(
                           "Use of the dist-datastore filter is deprecated. Please use the distributed datastore service.");
               }

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
         LOG.info("Problem loading the filter class. Just process the next filter. Reason: " + e.getMessage(), e);
      }

      return context;
   }
}
