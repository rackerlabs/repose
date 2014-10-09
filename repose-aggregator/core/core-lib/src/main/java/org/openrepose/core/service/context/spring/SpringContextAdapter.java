package org.openrepose.core.service.context.spring;

import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.core.service.classloader.ClassLoaderManagerService;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.context.ContextAdapter;
import org.openrepose.core.service.context.ServiceContext;
import org.openrepose.core.service.context.ServiceContextName;
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
import org.springframework.context.ApplicationContext;

public class SpringContextAdapter implements ContextAdapter {

   private final ApplicationContext applicationContext;

   public SpringContextAdapter(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
   }

   public <T> T getService(ServiceContextName context) {
      return ((ServiceContext<T>) applicationContext.getBean(context.getServiceContextName())).getService();
   }

   @Override
   public ClassLoaderManagerService classLoader() {
      return classLoaderContext().getService();
   }

   public ServiceContext<ClassLoaderManagerService> classLoaderContext() {
      return (ServiceContext<ClassLoaderManagerService>) applicationContext.getBean(ServiceContextName.CLASS_LOADER_SERVICE_CONTEXT.getServiceContextName());
   }

   @Override
   public String getReposeVersion() {
      return applicationContext.getBean("reposeVersion", String.class);
   }

   private String beanNameForClass(Class clazz) {
      String name = clazz != null ? clazz.getSimpleName() : "";
      if (name == null || name.length() == 0) {
         return "";
      }
      StringBuilder builder = new StringBuilder();

      builder.append(name.substring(0, 1).toLowerCase());
      if (name.length() > 1) {
         builder.append(name.substring(1));
      }

      return builder.toString();
   }

   @Override
   public <T extends ServiceContext<?>> T getContext(Class<T> clazz) {
      return applicationContext.getBean(beanNameForClass(clazz), clazz);
   }

   @Override
   public ConfigurationService configurationService() {
      return getService(ServiceContextName.CONFIGURATION_SERVICE_CONTEXT);
   }

   @Override
   public ContainerConfigurationService containerConfigurationService() {
      return getService(ServiceContextName.CONTAINER_SERVICE_CONTEXT);
   }

   @Override
   public DatastoreService datastoreService() {
      return getService(ServiceContextName.DATASTORE_SERVICE_CONTEXT);
   }

   @Override
   public EventService eventService() {
      return getService(ServiceContextName.EVENT_MANAGER_SERVICE_CONTEXT);
   }

   @Override
   public LoggingService loggingService() {
      return getService(ServiceContextName.LOGGING_SERVICE_CONTEXT);
   }

   @Override
   public MetricsService metricsService() {
      return getService(ServiceContextName.METRICS_SERVICE_CONTEXT);
   }

   @Override
   public <T> T filterChainBuilder() {
      return (T) applicationContext.getBean(ServiceContextName.POWER_FILTER_CHAIN_BUILDER.getServiceContextName());
   }

   @Override
   public ResponseMessageService responseMessageService() {
      return getService(ServiceContextName.RESPONSE_MESSAGE_SERVICE_CONTEXT);
   }

   @Override
   public RoutingService routingService() {
      return getService(ServiceContextName.ROUTING_SERVICE_CONTEXT);
   }

   @Override
   public ThreadingService threadingService() {
      return getService(ServiceContextName.THREADING_SERVICE_CONTEXT);
   }

   @Override
   public RequestProxyService requestProxyService() {
      return getService(ServiceContextName.REQUEST_PROXY_SERVICE_CONTEXT);
   }

    @Override
    public HttpClientService httpConnectionPoolService(){
        return getService(ServiceContextName.HTTP_CONNECTION_POOL_SERVICE_CONTEXT);
    }

    @Override
    public AkkaServiceClient akkaServiceClientService(){
        return getService(ServiceContextName.AKKA_SERVICE_CLIENT_SERVICE_CONTEXT);
    }

   @Override
   public ReportingService reportingService() {
      return getService(ServiceContextName.REPORTING_SERVICE_CONTEXT);
   }

   @Override
   public RequestHeaderService requestHeaderService() {
      return getService(ServiceContextName.REQUEST_HEADER_SERVICE_CONTEXT);
   }

   @Override
   public ResponseHeaderService responseHeaderService() {
      return getService(ServiceContextName.RESPONSE_HEADER_SERVICE_CONTEXT);
   }

   @Override
   public <T> T reposeConfigurationInformation() {
      return (T) applicationContext.getBean(ServiceContextName.REPOSE_CONFIGURATION_INFORMATION.getServiceContextName());
   }

   @Override
   public DistributedDatastoreLauncherService distributedDatastoreService() {
      return getService(ServiceContextName.DISTRIBUTED_DATASTORE_SERVICE_CONTEXT);
   }

   @Override
   public DistributedDatastoreServiceClusterViewService distributedDatastoreServiceClusterViewService() {
      return getService(ServiceContextName.DISTRIBUTED_DATASTORE_SERVICE_CLUSTER_CONTEXT);
   }

    @Override
    public HealthCheckService healthCheckService() {
        return getService(ServiceContextName.HEALTH_CHECK_SERVICE_CONTEXT);
    }

}
