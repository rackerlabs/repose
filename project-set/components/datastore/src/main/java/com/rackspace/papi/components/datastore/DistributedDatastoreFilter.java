package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
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
import java.util.UUID;
import javax.naming.NamingException;

public class DistributedDatastoreFilter implements Filter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DistributedDatastoreFilter.class);
   private final String datastoreId;
   private DatastoreFilterLogicHandlerFactory handlerFactory;
   private DatastoreService datastoreService;

   public DistributedDatastoreFilter() {
      this(HashRingDatastoreManager.DATASTORE_MANAGER_NAME + "-" + UUID.randomUUID().toString());
   }

   public DistributedDatastoreFilter(String datastoreId) {
      this.datastoreId = datastoreId;
   }

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
         final HashRingDatastoreManager hashRingDatastoreManager = new HashRingDatastoreManager("temp-host-key", UUIDEncodingProvider.getInstance(), MD5MessageDigestFactory.getInstance(), clusterView, datastoreService.defaultDatastore());
         hashRingDatastore = hashRingDatastoreManager.newDatastoreServer("default");

         datastoreService.registerDatastoreManager(datastoreId, hashRingDatastoreManager);

         handlerFactory = new DatastoreFilterLogicHandlerFactory(clusterView, hashRingDatastore);
         contextAdapter.configurationService().subscribeTo("power-proxy.cfg.xml", handlerFactory, PowerProxy.class);
      } catch (NamingException ne) {
         LOG.error(ne.getExplanation(), ne);
      }
   }

   @Override
   public void destroy() {
      try {
         datastoreService.unregisterDatastoreManager(datastoreId);
      } catch (NamingException ne) {
         LOG.error("Unable to unregister hash-ring datastore service. This may cause problems in component re-loads. Please log this as a bug. Reason: " + ne.getMessage(), ne);
      }
   }
}
