package org.openrepose.core.services.context;

import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.core.services.classloader.ClassLoaderManagerService;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.container.ContainerConfigurationService;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.DistributedDatastoreLauncherService;
import org.openrepose.core.services.datastore.distributed.impl.distributed.cluster.DistributedDatastoreServiceClusterViewService;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.headers.request.RequestHeaderService;
import org.openrepose.core.services.headers.response.ResponseHeaderService;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.httpclient.HttpClientService;
import org.openrepose.core.services.logging.LoggingService;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.rms.ResponseMessageService;
import org.openrepose.core.services.routing.RoutingService;
import org.openrepose.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.core.services.threading.ThreadingService;


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
