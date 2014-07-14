package com.rackspace.papi.service.context.spring;

import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServiceContextName;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.DistributedDatastoreLauncherService;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.DistributedDatastoreServiceClusterViewService;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.headers.request.RequestHeaderService;
import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.logging.LoggingService;
import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient;
import com.rackspace.papi.service.threading.ThreadingService;
import org.springframework.context.ApplicationContext;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Deprecated
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
       throw new NotImplementedException();
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
       throw new NotImplementedException();
   }

   @Override
   public ContainerConfigurationService containerConfigurationService() {
      return getService(ServiceContextName.CONTAINER_SERVICE_CONTEXT);
   }

   @Override
   public DatastoreService datastoreService() {
       throw new NotImplementedException();
   }

   @Override
   public EventService eventService() {
       throw new NotImplementedException();
   }

   @Override
   public LoggingService loggingService() {
       throw new NotImplementedException();
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
       throw new NotImplementedException();
   }

   @Override
   public RoutingService routingService() {
       throw new NotImplementedException();
   }

   @Override
   public ThreadingService threadingService() {
       throw new NotImplementedException();
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
       throw new NotImplementedException();
   }

   @Override
   public RequestHeaderService requestHeaderService() {
       throw new NotImplementedException();   }

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
       throw new NotImplementedException();
   }

    @Override
    public HealthCheckService healthCheckService() {
        return getService(ServiceContextName.HEALTH_CHECK_SERVICE_CONTEXT);
    }

}
