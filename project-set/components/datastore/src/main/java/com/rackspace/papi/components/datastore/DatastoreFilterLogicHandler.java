package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.datastore.common.CacheRequest;
import com.rackspace.papi.components.datastore.common.MalformedCacheRequestException;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dan Daley
 */
public class DatastoreFilterLogicHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(DatastoreFilterLogicHandler.class);
   private final MutableClusterView clusterView;
   private final DatastoreAccessControl hostAcl;
   private HashedDatastore hashRingDatastore;

   public DatastoreFilterLogicHandler(MutableClusterView clusterView, String lastLocalAddr, HashedDatastore hashRingDatastore, DatastoreAccessControl hostAcl) {
      this.clusterView = clusterView;
      this.hostAcl = hostAcl;
      this.hashRingDatastore = hashRingDatastore;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      FilterDirector director = new FilterDirectorImpl();
      director.setFilterAction(FilterAction.PASS);

      if (CacheRequest.isCacheRequest(request)) {
         if (isAllowed(request)) {
            clusterView.updateLocalAddress(new InetSocketAddress(request.getLocalAddr(), request.getLocalPort()));
            director = performCacheRequest(request);
         } else {
            director.setResponseStatus(HttpStatusCode.FORBIDDEN);
            director.setFilterAction(FilterAction.RETURN);
         }
      }

      return director;
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
            LOG.error("Unknown host exception caught while trying to resolve host: " + request.getRemoteHost());
         }
      }

      return allowed;
   }

   public FilterDirector performCacheRequest(HttpServletRequest request) {
      final FilterDirector director = new FilterDirectorImpl();

      // Defaults to return not-implemented
      director.setResponseStatus(HttpStatusCode.NOT_IMPLEMENTED);
      director.setFilterAction(FilterAction.RETURN);

      try {
         final String requestMethod = request.getMethod();

         if ("GET".equalsIgnoreCase(requestMethod)) {
            onCacheGet(CacheRequest.marshallCacheGetRequest(request), director);
         } else if ("PUT".equalsIgnoreCase(requestMethod)) {
            onCachePut(request, director);
         } else if ("DELETE".equalsIgnoreCase(requestMethod)) {
            onCacheDelete(request, director);
         }
      } catch (MalformedCacheRequestException mcre) {
         LOG.error(mcre.getMessage(), mcre);

         director.getResponseWriter().write(mcre.getMessage() == null ? "" : mcre.getMessage());
         director.setResponseStatus(HttpStatusCode.BAD_REQUEST);
         director.setFilterAction(FilterAction.RETURN);
      } catch (Exception ex) {
         LOG.error(ex.getMessage(), ex);

         director.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
         director.setFilterAction(FilterAction.RETURN);
      }

      return director;
   }

   public void onCacheDelete(HttpServletRequest request, final FilterDirector director) throws DatastoreOperationException, MalformedCacheRequestException {
      final CacheRequest cacheDelete = CacheRequest.marshallCacheGetRequest(request);
      hashRingDatastore.removeByHash(cacheDelete.getCacheKey());

      director.setResponseStatus(HttpStatusCode.ACCEPTED);
      director.setFilterAction(FilterAction.RETURN);
   }

   public void onCachePut(HttpServletRequest request, final FilterDirector director) throws MalformedCacheRequestException, DatastoreOperationException {
      final CacheRequest cachePut = CacheRequest.marshallCachePutRequest(request);
      hashRingDatastore.putByHash(cachePut.getCacheKey(), cachePut.getPayload(), cachePut.getTtlInSeconds(), TimeUnit.SECONDS);

      director.setResponseStatus(HttpStatusCode.ACCEPTED);
      director.setFilterAction(FilterAction.RETURN);
   }

   public void onCacheGet(CacheRequest cacheGet, FilterDirector director) {
      final StoredElement element = hashRingDatastore.getByHash(cacheGet.getCacheKey());

      if (!element.elementIsNull()) {
         try {
            director.getResponseOutputStream().write(element.elementBytes());

            director.setResponseStatus(HttpStatusCode.OK);
            director.setFilterAction(FilterAction.RETURN);
         } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);

            director.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            director.setFilterAction(FilterAction.RETURN);
         }
      } else {
         director.setResponseStatus(HttpStatusCode.NOT_FOUND);
         director.setFilterAction(FilterAction.RETURN);
      }
   }
}
