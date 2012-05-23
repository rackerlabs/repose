package org.openrepose.components.rackspace.authz;

import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;

import javax.servlet.*;
import java.io.IOException;

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
      final DatastoreService datastoreService = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).datastoreService();
      final DatastoreManager defaultLocal = datastoreService.defaultDatastore();

      final ConfigurationService configurationService = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
      handlerFactory = new RequestAuthorizationHandlerFactory(defaultLocal.getDatastore());

      configurationService.subscribeTo("openstack-authorization.cfg.xml", handlerFactory, RackspaceAuthorization.class);
   }
}
