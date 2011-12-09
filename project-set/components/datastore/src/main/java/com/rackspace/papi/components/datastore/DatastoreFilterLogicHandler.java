/*
 *
 */
package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import java.io.IOException;
import java.net.InetSocketAddress;
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
    private String lastLocalAddr;
    private HashedDatastore hashRingDatastore;
    
    public DatastoreFilterLogicHandler(MutableClusterView clusterView, String lastLocalAddr, HashedDatastore hashRingDatastore) {
       this.clusterView = clusterView;
       this.lastLocalAddr = lastLocalAddr;
       this.hashRingDatastore = hashRingDatastore;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector director = new FilterDirectorImpl();
        director.setFilterAction(FilterAction.PASS);

        updateClusterViewLocalAddress(request.getLocalAddr(), request.getLocalPort());

        if (CacheRequest.isCacheRequest(request)) {
            final String requestMethod = request.getMethod();

            try {
                if (requestMethod.equalsIgnoreCase("GET")) {
                    onCacheGet(CacheRequest.marshallCacheGetRequest(request), director);
                } else if (requestMethod.equalsIgnoreCase("PUT")) {
                    final CacheRequest cachePut = CacheRequest.marshallCachePutRequest(request);
                    hashRingDatastore.putByHash(cachePut.getCacheKey(), cachePut.getPayload(), cachePut.getTtlInSeconds(), TimeUnit.SECONDS);

                    director.setResponseStatus(HttpStatusCode.ACCEPTED);
                    director.setFilterAction(FilterAction.RETURN);
                } else if (requestMethod.equalsIgnoreCase("DELETE")) {
                    director.setResponseStatus(HttpStatusCode.NOT_IMPLEMENTED);
                    director.setFilterAction(FilterAction.RETURN);
                } else {
                    director.setResponseStatus(HttpStatusCode.NOT_IMPLEMENTED);
                    director.setFilterAction(FilterAction.RETURN);
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
        }

        return director;
    }
   
    public void updateClusterViewLocalAddress(String newLocalAddr, int newLocalPort) {
        if (lastLocalAddr == null || !lastLocalAddr.equals(newLocalAddr)) {
            // String immutability should make this assignment safe
            lastLocalAddr = newLocalAddr;

            final InetSocketAddress localInetSocketAddress = new InetSocketAddress(newLocalAddr, newLocalPort);

            if (!localInetSocketAddress.equals(clusterView.localMember())) {
                clusterView.updateLocal(localInetSocketAddress);
            }
        }
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
