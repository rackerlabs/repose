package org.openrepose.components.rackspace.authz;

import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;

public class RackspaceAuthorizationFilter implements Filter {

   private RequestAuthorizationHandlerFactory handlerFactory;

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
   }

   @Override
   public void destroy() {
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final DatastoreService datastoreService = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).datastoreService();
      final DatastoreManager defaultLocal = datastoreService.defaultDatastore();

      final ConfigurationService configurationService = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
      handlerFactory = new RequestAuthorizationHandlerFactory(defaultLocal.getDatastore());

      configurationService.subscribeTo("openstack-authorization.cfg.xml", handlerFactory, RackspaceAuthorization.class);
   }
}
