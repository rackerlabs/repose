package com.rackspace.papi.service.context;

import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.DistributedDatastoreLauncherService;
import com.rackspace.papi.service.datastore.impl.distributed.cluster.DistributedDatastoreServiceClusterViewService;
import com.rackspace.papi.service.datastore.impl.distributed.cluster.DistributedDatastoreServiceClusterViewServiceImpl;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.headers.request.RequestHeaderService;
import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.logging.LoggingService;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.service.threading.ThreadingService;

public interface ContextAdapter {

    ClassLoaderManagerService classLoader();
    EventService eventService();
    ThreadingService threadingService();
    DatastoreService datastoreService();
    ConfigurationService configurationService();
    ContainerConfigurationService containerConfigurationService();
    GarbageCollectionService filterChainGarbageCollectorService();
    ResponseMessageService responseMessageService();
    LoggingService loggingService();
    MetricsService metricsService();
    RoutingService routingService();
    RequestProxyService requestProxyService();
    ReportingService reportingService();
    String getReposeVersion();
    RequestHeaderService requestHeaderService();
    ResponseHeaderService responseHeaderService();
    DistributedDatastoreLauncherService distributedDatastoreService();
    DistributedDatastoreServiceClusterViewService distributedDatastoreServiceClusterViewService();
    <T> T filterChainBuilder();
    <T> T  reposeConfigurationInformation();
    
   <T extends ServiceContext<?>> T getContext(Class<T> clazz);

}
