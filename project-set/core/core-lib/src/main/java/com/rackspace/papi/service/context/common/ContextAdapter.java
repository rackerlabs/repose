package com.rackspace.papi.service.context.common;

import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.logging.LoggingService;
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
    RoutingService routingService();
    
   <T extends ServiceContext<?>> T getContext(Class<T> clazz);

}
