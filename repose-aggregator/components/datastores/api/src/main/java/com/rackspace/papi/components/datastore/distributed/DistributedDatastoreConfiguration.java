package com.rackspace.papi.components.datastore.distributed;

import com.rackspace.papi.commons.util.encoding.EncodingProvider;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;

public class DistributedDatastoreConfiguration {

    private RequestProxyService proxyService;
    private EncodingProvider encodingProvider;
    private ClusterView clusterView;

    public DistributedDatastoreConfiguration(RequestProxyService proxyService, EncodingProvider encodingProvider, ClusterView clusterView) {
        this.proxyService = proxyService;
        this.encodingProvider = encodingProvider;
        this.clusterView = clusterView;
    }

    public RequestProxyService getProxyService() {
        return proxyService;
    }

    public EncodingProvider getEncodingProvider() {
        return encodingProvider;
    }

    public ClusterView getClusterView() {
        return clusterView;
    }
}
