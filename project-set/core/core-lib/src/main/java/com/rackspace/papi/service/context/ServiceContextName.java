package com.rackspace.papi.service.context;

public enum ServiceContextName {

    CLASS_LOADER_SERVICE_CONTEXT("classLoaderServiceContext"),
    CONFIGURATION_SERVICE_CONTEXT("configurationServiceContext"),
    CONTAINER_SERVICE_CONTEXT("containerServiceContext"),
    DATASTORE_SERVICE_CONTEXT("datastoreServiceContext"),
    EVENT_MANAGER_SERVICE_CONTEXT("eventManagerServiceContext"),
    FILTER_CHAIN_GC_SERVICE_CONTEXT("filterChainGCServiceContext"),
    LOGGING_SERVICE_CONTEXT("loggingServiceContext"),
    METRICS_SERVICE_CONTEXT( "metricsServiceContext" ),
    RESPONSE_MESSAGE_SERVICE_CONTEXT("responseMessageServiceContext"),
    ROUTING_SERVICE_CONTEXT("routingServiceContext"),
    THREADING_SERVICE_CONTEXT("threadingServiceContext"),
    REQUEST_PROXY_SERVICE_CONTEXT("requestProxyServiceContext"),
    REPORTING_SERVICE_CONTEXT("reportingServiceContext"),
    REQUEST_HEADER_SERVICE_CONTEXT("requestHeaderServiceContext"),
    RESPONSE_HEADER_SERVICE_CONTEXT("responseHeaderServiceContext"),
    POWER_FILTER_CHAIN_BUILDER("powerFilterChainBuilder"),
    REPOSE_CONFIGURATION_INFORMATION("reposeConfigurationInformation"),
    DISTRIBUTED_DATASTORE_SERVICE_CONTEXT("distributedDatastoreServiceContext"),
    DISTRIBUTED_DATASTORE_SERVICE_CLUSTER_CONTEXT("distributedDatastoreServiceClusterContext"),;
    private String serviceContextName;

    ServiceContextName(String serviceContextName) {
        this.serviceContextName = serviceContextName;
    }

    public String getServiceContextName() {
        return serviceContextName;
    }
}
