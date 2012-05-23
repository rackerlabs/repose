package com.rackspace.papi.service.context.spring;

import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.context.impl.*;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.logging.LoggingService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.service.threading.ThreadingService;
import com.rackspace.papi.service.threading.impl.ThreadingServiceContext;
import org.springframework.context.ApplicationContext;

public class SpringContextAdapter implements ContextAdapter {
   public static final String CLASS_LOADER_SERVICE_CONTEXT = "classLoaderServiceContext";
   public static final String CONFIGURATION_SERVICE_CONTEXT = "configurationServiceContext";
   public static final String CONTAINER_SERVICE_CONTEXT = "containerServiceContext";
   public static final String DATASTORE_SERVICE_CONTEXT = "datastoreServiceContext";
   public static final String EVENT_MANAGER_SERVICE_CONTEXT = "eventManagerServiceContext";
   public static final String FILTER_CHAIN_GC_SERVICE_CONTEXT = "filterChainGCServiceContext";
   public static final String LOGGING_SERVICE_CONTEXT = "loggingServiceContext";
   public static final String RESPONSE_MESSAGE_SERVICE_CONTEXT = "responseMessageServiceContext";
   public static final String ROUTING_SERVICE_CONTEXT = "routingServiceContext";
   public static final String THREADING_SERVICE_CONTEXT = "threadingServiceContext";
   
   private final ApplicationContext applicationContext;
   
   public SpringContextAdapter(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
   }

   @Override
   public ClassLoaderManagerService classLoader() {
      return classLoaderContext().getService();
   }
   
   public ServiceContext<ClassLoaderManagerService> classLoaderContext() {
      return applicationContext.getBean(CLASS_LOADER_SERVICE_CONTEXT, ClassLoaderServiceContext.class);
   }
   
   private String beanNameForClass(Class clazz) {
      String name = clazz != null? clazz.getSimpleName(): "";
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
      return applicationContext.getBean(CONFIGURATION_SERVICE_CONTEXT, ConfigurationServiceContext.class).getService();
   }

   @Override
   public ContainerConfigurationService containerConfigurationService() {
      return applicationContext.getBean(CONTAINER_SERVICE_CONTEXT, ContainerServiceContext.class).getService();
   }

   @Override
   public DatastoreService datastoreService() {
      return applicationContext.getBean(DATASTORE_SERVICE_CONTEXT, DatastoreServiceContext.class).getService();
   }

   @Override
   public EventService eventService() {
      return applicationContext.getBean(EVENT_MANAGER_SERVICE_CONTEXT, EventManagerServiceContext.class).getService();
   }

   @Override
   public GarbageCollectionService filterChainGarbageCollectorService() {
      return applicationContext.getBean(FILTER_CHAIN_GC_SERVICE_CONTEXT, FilterChainGCServiceContext.class).getService();
   }

   @Override
   public LoggingService loggingService() {
      return applicationContext.getBean(LOGGING_SERVICE_CONTEXT, LoggingServiceContext.class).getService();
   }

   @Override
   public ResponseMessageService responseMessageService() {
      return applicationContext.getBean(RESPONSE_MESSAGE_SERVICE_CONTEXT, ResponseMessageServiceContext.class).getService();
   }

   @Override
   public RoutingService routingService() {
      return applicationContext.getBean(ROUTING_SERVICE_CONTEXT, RoutingServiceContext.class).getService();
   }
   
   @Override
   public ThreadingService threadingService() {
      return applicationContext.getBean(THREADING_SERVICE_CONTEXT, ThreadingServiceContext.class).getService();
   }

}
