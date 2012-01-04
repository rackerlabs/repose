package com.rackspace.papi.service.context.jndi;

import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.logging.LoggingService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.ServiceUnavailableException;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.threading.ThreadingService;

public interface ContextAdapter {

    ClassLoaderManagerService classLoader() throws ServiceUnavailableException;

    EventService eventService() throws ServiceUnavailableException;

    ThreadingService threadingService() throws ServiceUnavailableException;

    DatastoreService datastoreService() throws ServiceUnavailableException;

    ConfigurationService configurationService() throws ServiceUnavailableException;

    GarbageCollectionService filterChainGarbageCollectorService() throws ServiceUnavailableException;

    ResponseMessageService responseMessageService() throws ServiceUnavailableException;

    LoggingService loggingService() throws ServiceUnavailableException;
}
