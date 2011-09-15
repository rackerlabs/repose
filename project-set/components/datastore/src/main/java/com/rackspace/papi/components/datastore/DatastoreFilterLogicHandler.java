package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.HashedDatastore;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DatastoreFilterLogicHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreFilterLogicHandler.class);
    private final UpdateListener<PowerProxy> systemModelUpdateListener;
    private final DatastoreService datastoreService;
    private final MutableClusterView clusterView;
    private String lastLocalAddr;
    private HashRingDatastoreManager hashRingDatastoreManager;
    private HashedDatastore hashRingDatastore;
    private class SystemModelUpdateListener implements UpdateListener<PowerProxy> {
        @Override
        public void configurationUpdated(PowerProxy configurationObject) {
            if (configurationObject == null) {
                LOG.error("Power Proxy configuration was null - please check your configurations and error logs");
                return;
            }

            try {
                final List<InetSocketAddress> cacheSiblings = new LinkedList<InetSocketAddress>();

                for (Host hostInformation : configurationObject.getHost()) {
                    if (hostInformation.getFilters() != null) {
                        for (Filter f : hostInformation.getFilters().getFilter()) {
                            if (f.getName().equals("dist-datastore")) {
                                final InetAddress hostAddress = InetAddress.getByName(hostInformation.getHostname());
                                final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, hostInformation.getServicePort());

                                cacheSiblings.add(hostSocketAddress);
                            }
                        }
                    }
                }

                clusterView.updateMembers(cacheSiblings.toArray(new InetSocketAddress[cacheSiblings.size()]));
            } catch (UnknownHostException uhe) {
                LOG.error(uhe.getMessage(), uhe);
            }

            if (hashRingDatastoreManager == null) {
                hashRingDatastoreManager = new HashRingDatastoreManager("temp-host-key", clusterView, datastoreService.defaultDatastore());
                hashRingDatastore = hashRingDatastoreManager.getDatastore("default");

                try {
                    datastoreService.registerDatastoreManager(HashRingDatastoreManager.DATASTORE_MANAGER_NAME, hashRingDatastoreManager);
                } catch (NamingException ne) {
                    LOG.error(ne.getExplanation(), ne);
                }
            }
        }
    }

    public DatastoreFilterLogicHandler(DatastoreService ds) {
        datastoreService = ds;
        clusterView = new ThreadSafeClusterView();
        systemModelUpdateListener = new SystemModelUpdateListener();
    }

    public UpdateListener<PowerProxy> getSystemModelUpdateListener() {
        return systemModelUpdateListener;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request) {
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
