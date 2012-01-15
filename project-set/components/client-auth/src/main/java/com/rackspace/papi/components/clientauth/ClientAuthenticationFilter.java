package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 *
 * @author jhopper
 */
public class ClientAuthenticationFilter implements Filter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationFilter.class);
   private ClientAuthenticationHandlerFactory handlerFactory;
   private ConfigurationService configurationManager;

   @Override
   public void destroy() {
      configurationManager.unsubscribeFrom("client-auth-n.cfg.xml", handlerFactory);
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);

      FilterDirector director = handlerFactory.newHandler().handleRequest(mutableHttpRequest, mutableHttpResponse);

      director.applyTo(mutableHttpRequest);

      switch (director.getFilterAction()) {
         case PASS:
            // This logic is replicated because it emulates old, expected behavior,
            chain.doFilter(mutableHttpRequest, mutableHttpResponse);
            director = handlerFactory.newHandler().handleResponse(mutableHttpRequest, mutableHttpResponse);
            director.applyTo(mutableHttpResponse);
            break;

         case PROCESS_RESPONSE:
            chain.doFilter(mutableHttpRequest, mutableHttpResponse);
            director = handlerFactory.newHandler().handleResponse(mutableHttpRequest, mutableHttpResponse);

         case RETURN:
         default:
            director.applyTo(mutableHttpResponse);
            break;
      }
   }

   private Datastore getDatastore(DatastoreService datastoreService) {
      return datastoreService.defaultDatastore().getDatastore();
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final ContextAdapter ctx = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext());
      
      handlerFactory = new ClientAuthenticationHandlerFactory(getDatastore(ctx.datastoreService()));
      configurationManager = ctx.configurationService();
      configurationManager.subscribeTo("client-auth-n.cfg.xml", handlerFactory, ClientAuthConfig.class);
   }
}
