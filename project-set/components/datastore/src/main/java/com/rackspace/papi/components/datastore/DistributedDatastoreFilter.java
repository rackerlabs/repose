package com.rackspace.papi.components.datastore;

import com.rackspace.papi.components.datastore.hash.HashRingDatastore;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.context.common.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import org.openrepose.components.datastore.config.DistributedDatastoreConfiguration;

import javax.servlet.*;
import java.io.IOException;
import java.util.Collection;

public class DistributedDatastoreFilter implements Filter {
   
   private final String datastoreId;
   private DatastoreFilterLogicHandlerFactory handlerFactory;
   private DatastoreService datastoreService;
   
   public DistributedDatastoreFilter() {
      this(HashRingDatastoreManager.DATASTORE_MANAGER_NAME);
   }
   
   public DistributedDatastoreFilter(String datastoreId) {
      this.datastoreId = datastoreId;
   }
   
   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
   }
   
   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final ContextAdapter contextAdapter = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext());
      datastoreService = contextAdapter.datastoreService();
      
      final MutableClusterView clusterView = new ThreadSafeClusterView(ServletContextHelper.getInstance().getServerPorts(filterConfig.getServletContext()));
      final HashRingDatastore hashRingDatastore;
      
      DatastoreManager localDatastoreManager = datastoreService.defaultDatastore();
      
      if (localDatastoreManager == null || !localDatastoreManager.isAvailable()) {
         final Collection<DatastoreManager> availableLocalDatstores = datastoreService.availableLocalDatastores();
         
         if (!availableLocalDatstores.isEmpty()) {
            localDatastoreManager = availableLocalDatstores.iterator().next();
         } else {
            throw new ServletException("Unable to start DistributedDatastoreFilter. Reason: no available local datastores to persist distributed data.");
         }
      }
      
      final HashRingDatastoreManager hashRingDatastoreManager = new HashRingDatastoreManager("", UUIDEncodingProvider.getInstance(), MD5MessageDigestFactory.getInstance(), clusterView, localDatastoreManager.getDatastore());
      hashRingDatastore = (HashRingDatastore) hashRingDatastoreManager.getDatastore();
      
      datastoreService.registerDatastoreManager(datastoreId, hashRingDatastoreManager);
      
      handlerFactory = new DatastoreFilterLogicHandlerFactory(clusterView, hashRingDatastore);
      
      contextAdapter.configurationService().subscribeTo("system-model.cfg.xml", handlerFactory, SystemModel.class);
      contextAdapter.configurationService().subscribeTo("dist-datastore.cfg.xml", handlerFactory, DistributedDatastoreConfiguration.class);
   }
   
   @Override
   public void destroy() {
      datastoreService.unregisterDatastoreManager(datastoreId);
   }
}
