package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 *
 * @author jhopper
 */
public class ClientAuthenticationFilter implements Filter {

   private ClientAuthenticationHandlerFactory handlerFactory;
   private ConfigurationService configurationManager;

   @Override
   public void destroy() {
      configurationManager.unsubscribeFrom("client-auth-n.cfg.xml", handlerFactory);
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
   }

   private Datastore getDatastore(DatastoreService datastoreService) {
      return datastoreService.defaultDatastore().getDatastore();
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final ContextAdapter ctx = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext());
      
      handlerFactory = new ClientAuthenticationHandlerFactory(getDatastore(ctx.datastoreService()));
      configurationManager = ctx.configurationService();
      configurationManager.subscribeTo("client-auth-n.cfg.xml", handlerFactory, ClientAuthConfig.class);
   }
}
