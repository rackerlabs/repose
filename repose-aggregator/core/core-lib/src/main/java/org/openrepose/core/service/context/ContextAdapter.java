package org.openrepose.core.service.context;

import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.core.service.classloader.ClassLoaderManagerService;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.context.container.ContainerConfigurationService;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.core.service.datastore.DistributedDatastoreLauncherService;
import org.openrepose.core.service.datastore.distributed.impl.distributed.cluster.DistributedDatastoreServiceClusterViewService;
import org.openrepose.core.service.event.common.EventService;
import org.openrepose.core.service.headers.request.RequestHeaderService;
import org.openrepose.core.service.headers.response.ResponseHeaderService;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.httpclient.HttpClientService;
import org.openrepose.core.service.logging.LoggingService;
import org.openrepose.core.service.reporting.ReportingService;
import org.openrepose.core.service.reporting.metrics.MetricsService;
import org.openrepose.core.service.rms.ResponseMessageService;
import org.openrepose.core.service.routing.RoutingService;
import org.openrepose.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.core.service.threading.ThreadingService;


public interface ContextAdapter {

    ClassLoaderManagerService classLoader();
    EventService eventService();
    ThreadingService threadingService();
    DatastoreService datastoreService();
    ConfigurationService configurationService();
    ContainerConfigurationService containerConfigurationService();
    ResponseMessageService responseMessageService();
    LoggingService loggingService();
    MetricsService metricsService();
    RoutingService routingService();
    RequestProxyService requestProxyService();
    ReportingService reportingService();
    String getReposeVersion();
    HttpClientService httpConnectionPoolService();
    AkkaServiceClient akkaServiceClientService();
    RequestHeaderService requestHeaderService();
    ResponseHeaderService responseHeaderService();
    DistributedDatastoreLauncherService distributedDatastoreService();
    DistributedDatastoreServiceClusterViewService distributedDatastoreServiceClusterViewService();
    HealthCheckService healthCheckService();
    <T> T filterChainBuilder();
    <T> T  reposeConfigurationInformation();
    
   <T extends ServiceContext<?>> T getContext(Class<T> clazz);

}
