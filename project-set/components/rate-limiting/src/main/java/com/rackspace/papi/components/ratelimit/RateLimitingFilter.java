package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.slf4j.Logger;

import javax.servlet.*;
import java.io.IOException;
import java.util.Collection;

/**
 *
 * @author jhopper
 */
public class RateLimitingFilter implements Filter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingFilter.class);
   private RateLimitingHandlerFactory handlerFactory;

   @Override
   public void destroy() {
      // TODO: In order to unsubscribe from configuration updates we need access to the configuration manager.
      // Do we want to keep the ConfigurationManager as a private variable that gets set on initialization?
//        configurationManager().unsubscribeFrom("rate-limiting.cfg.xml", handler.getRateLimitingConfigurationListener());
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
   }

   private Datastore getDatastore(DatastoreService datastoreService) {
      Datastore targetDatastore;

      final Collection<DatastoreManager> distributedDatastores = datastoreService.availableDistirbutedDatastores();
      
      if (!distributedDatastores.isEmpty()) {
         targetDatastore = distributedDatastores.iterator().next().getDatastore();
      } else {
         LOG.warn("There were no distributed datastore managers available. Clustering for rate-limiting will be disabled.");
         targetDatastore = datastoreService.defaultDatastore().getDatastore();
      }
      
      return targetDatastore;
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final ContextAdapter ctx = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext());

      handlerFactory = new RateLimitingHandlerFactory(getDatastore(ctx.datastoreService()));

      ctx.configurationService().subscribeTo("rate-limiting.cfg.xml", handlerFactory, RateLimitingConfiguration.class);
   }
}
