package com.rackspace.papi.service.datastore.impl.distributed.servlet;

import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.impl.PowerApiContextManager;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import com.rackspace.papi.service.datastore.impl.distributed.common.CacheRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import com.rackspace.papi.service.datastore.impl.distributed.cluster.DistributedDatastoreServiceClusterViewService;
import com.rackspace.papi.service.datastore.impl.distributed.hash.HashRingDatastore;
import com.rackspace.papi.service.datastore.impl.distributed.hash.HashRingDatastoreManager;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class DistributedDatastoreServlet extends HttpServlet {

   private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreServlet.class);
   private DatastoreAccessControl hostAcl;
   private HashRingDatastore hashRingDatastore;
   private EncodingProvider encodingProvider;
   DatastoreService datastore;
   private DistributedDatastoreServiceClusterViewService clusterView;
   private static final String DATASTORE_MANAGER_NAME = "distributed/hash-ring";

   public DistributedDatastoreServlet(DatastoreService datastore) {
      hostAcl = new DatastoreAccessControl(null, false);
      this.datastore = datastore;
      encodingProvider = UUIDEncodingProvider.getInstance();
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

      if (isRequestValid(req,resp)) {
         CacheRequest cacheGet = CacheRequest.marshallCacheRequest(req);
         final StoredElement element = hashRingDatastore.get(cacheGet.getCacheKey(), encodingProvider.decode(cacheGet.getCacheKey()), cacheGet.getRequestedRemoteBehavior());

         if (!element.elementIsNull()) {
            try {
               resp.getOutputStream().write(element.elementBytes());
               resp.setStatus(HttpServletResponse.SC_OK);

            } catch (IOException ioe) {
               LOG.error(ioe.getMessage(), ioe);
               resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            }
         } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
         }
      }
   }

   @Override
   public void init(ServletConfig config) throws ServletException {

      super.init(config);
      DatastoreManager localDatastoreManager = datastore.defaultDatastore();

      if (localDatastoreManager == null || !localDatastoreManager.isAvailable()) {
         final Collection<DatastoreManager> availableLocalDatstores = datastore.availableLocalDatastores();

         if (!availableLocalDatstores.isEmpty()) {
            localDatastoreManager = availableLocalDatstores.iterator().next();
         } else {
            throw new ServletException("Unable to start Distributed Datastore Service");
         }
      }



      ContextAdapter contextAdapter = ServletContextHelper.getInstance(config.getServletContext()).getPowerApiContext();
      clusterView = contextAdapter.distributedDatastoreServiceClusterViewService();
      HashRingDatastoreManager manager = new HashRingDatastoreManager(contextAdapter.requestProxyService(), "", encodingProvider, MD5MessageDigestFactory.getInstance(),
              clusterView.getClusterView(), localDatastoreManager.getDatastore());
      hostAcl = clusterView.getAccessControl();
      datastore.registerDatastoreManager(DATASTORE_MANAGER_NAME, manager);
      hashRingDatastore = (HashRingDatastore) manager.getDatastore();
   }

   @Override
   protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
      if (CacheRequest.isCacheRequest(req)) {
         final CacheRequest cachePut = CacheRequest.marshallCachePutRequest(req);
         hashRingDatastore.put(cachePut.getCacheKey(), encodingProvider.decode(cachePut.getCacheKey()), cachePut.getPayload(), cachePut.getTtlInSeconds(), TimeUnit.SECONDS, cachePut.getRequestedRemoteBehavior());
         resp.setStatus(HttpServletResponse.SC_ACCEPTED);
      } else {
         resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
   }

   @Override
   protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
      if (CacheRequest.isCacheRequest(req)) {
         final CacheRequest cacheDelete = CacheRequest.marshallCacheRequest(req);
         hashRingDatastore.remove(cacheDelete.getCacheKey(), encodingProvider.decode(cacheDelete.getCacheKey()), cacheDelete.getRequestedRemoteBehavior());
         resp.setStatus(HttpServletResponse.SC_ACCEPTED);
      }else
      {
         resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
      //TODO: Move over hashring datastore and perform object remove
      
   }

   public boolean isAllowed(HttpServletRequest request) {
      boolean allowed = hostAcl.shouldAllowAll();

      if (!allowed) {
         try {
            final InetAddress remoteClient = InetAddress.getByName(request.getRemoteHost());

            for (InetAddress allowedAddress : hostAcl.getAllowedHosts()) {
               if (remoteClient.equals(allowedAddress)) {
                  allowed = true;
                  break;
               }
            }
         } catch (UnknownHostException uhe) {
            LOG.error("Unknown host exception caught while trying to resolve host: " + request.getRemoteHost() + " Reason: " + uhe.getMessage(), uhe);
         }
      }

      return allowed;
   }
   
   private boolean isRequestValid(HttpServletRequest req, HttpServletResponse resp){
      boolean valid = false;
      if(!isAllowed(req)){
         resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      }else if(!CacheRequest.isCacheRequest(req)){
         resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }else{
         valid = true;
      }
      
      return valid;
   }
   
   @Override
   public void destroy(){
      super.destroy();
      LOG.info("Unregistering Datastore: " + DATASTORE_MANAGER_NAME);
      datastore.unregisterDatastoreManager(DATASTORE_MANAGER_NAME);
   }
}
