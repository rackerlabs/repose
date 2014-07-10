package com.rackspace.papi.service.context;

public enum ServiceContextName {

    CONFIGURATION_SERVICE_CONTEXT("configurationServiceContext"),
    CONTAINER_SERVICE_CONTEXT("containerServiceContext"),
    METRICS_SERVICE_CONTEXT( "metricsServiceContext" ),
    HTTP_CONNECTION_POOL_SERVICE_CONTEXT("httpConnectionPoolServiceContext"),
    AKKA_SERVICE_CLIENT_SERVICE_CONTEXT("akkaServiceClientContext"),
    REQUEST_PROXY_SERVICE_CONTEXT("requestProxyServiceContext"),
    RESPONSE_HEADER_SERVICE_CONTEXT("responseHeaderServiceContext"),
    POWER_FILTER_CHAIN_BUILDER("powerFilterChainBuilder"),
    REPOSE_CONFIGURATION_INFORMATION("reposeConfigurationInformation"),
    DISTRIBUTED_DATASTORE_SERVICE_CONTEXT("distributedDatastoreServiceContext"),
    HEALTH_CHECK_SERVICE_CONTEXT("healthCheckServiceContext");
    private String serviceContextName;

    ServiceContextName(String serviceContextName) {
        this.serviceContextName = serviceContextName;
    }

    public String getServiceContextName() {
        return serviceContextName;
    }
}
