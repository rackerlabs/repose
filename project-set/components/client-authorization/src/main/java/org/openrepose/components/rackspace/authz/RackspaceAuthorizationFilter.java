package org.openrepose.components.rackspace.authz;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;
import org.slf4j.LoggerFactory;

public class RackspaceAuthorizationFilter implements Filter {

   private static final Logger LOG = LoggerFactory.getLogger(RackspaceAuthorizationFilter.class);
   private RequestAuthroizationHandlerFactory handlerFactory;

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);

      final FilterDirector director = handlerFactory.newHandler().handleRequest(mutableHttpRequest, mutableHttpResponse);

      director.applyTo(mutableHttpRequest);

      switch (director.getFilterAction()) {
         case RETURN:
            director.applyTo(mutableHttpResponse);
            break;
            
         case PASS:
            chain.doFilter(mutableHttpRequest, response);
            break;
      }
   }

   @Override
   public void destroy() {
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final DatastoreService datastoreService = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).datastoreService();
      final DatastoreManager defaultLocal = datastoreService.defaultDatastore();

      final ConfigurationService configurationService = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
      handlerFactory = new RequestAuthroizationHandlerFactory(defaultLocal.getDatastore());

      configurationService.subscribeTo("rackspace-authorization.cfg.xml", handlerFactory, RackspaceAuthorization.class);
   }
}
