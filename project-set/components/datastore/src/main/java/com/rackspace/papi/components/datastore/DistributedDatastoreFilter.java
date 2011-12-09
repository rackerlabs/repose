package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashProvider;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.hash.MD5HashProvider;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.naming.NamingException;

public class DistributedDatastoreFilter implements Filter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DistributedDatastoreFilter.class);
   private DatastoreFilterLogicHandlerFactory handlerFactory;
   private DatastoreService datastoreService;

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

      final HttpServletResponse httpResponse = (HttpServletResponse) response;
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);

      final FilterDirector director = handlerFactory.newHandler().handleRequest((HttpServletRequest) request, mutableHttpResponse);

      switch (director.getFilterAction()) {
         case PASS:
         case NOT_SET:
            chain.doFilter(request, response);
            break;

         case RETURN:
         case PROCESS_RESPONSE:
            director.applyTo(httpResponse);
            break;
      }
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final ContextAdapter contextAdapter = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext());
      datastoreService = contextAdapter.datastoreService();

      final MutableClusterView clusterView = new ThreadSafeClusterView();
      final HashedDatastore hashRingDatastore;

      try {
         final HashProvider hashProvider = new MD5HashProvider();

         final HashRingDatastoreManager hashRingDatastoreManager = new HashRingDatastoreManager("temp-host-key", UUIDEncodingProvider.getInstance(), hashProvider, clusterView, datastoreService.defaultDatastore());
         hashRingDatastore = hashRingDatastoreManager.newDatastoreServer("default");

         datastoreService.registerDatastoreManager(HashRingDatastoreManager.DATASTORE_MANAGER_NAME, hashRingDatastoreManager);

         handlerFactory = new DatastoreFilterLogicHandlerFactory(clusterView, hashRingDatastore);
         contextAdapter.configurationService().subscribeTo("power-proxy.cfg.xml", handlerFactory, PowerProxy.class);
      } catch (NoSuchAlgorithmException algorithmException) {
         LOG.error("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);

         throw new DatastoreOperationException("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);
      } catch (NamingException ne) {
         LOG.error(ne.getExplanation(), ne);
      }
   }

   @Override
   public void destroy() {
      try {
         datastoreService.unregisterDatastoreManager(HashRingDatastoreManager.DATASTORE_MANAGER_NAME);
      } catch (NamingException ne) {
         LOG.error("Unable to unregister hash-ring datastore service. This may cause problems in component re-loads. Please log this as a bug. Reason: " + ne.getMessage(), ne);
      }
   }
}
