package com.rackspace.papi.service.context.jndi;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ClassLoaderServiceContext;
import com.rackspace.papi.service.context.LoggingServiceContext;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.logging.LoggingService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.ServiceUnavailableException;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.context.EventManagerServiceContext;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.context.ConfigurationServiceContext;
import com.rackspace.papi.service.context.ContainerServiceContext;
import com.rackspace.papi.service.context.ResponseMessageServiceContext;
import com.rackspace.papi.service.context.DatastoreServiceContext;
import com.rackspace.papi.service.context.FilterChainGCServiceContext;
import com.rackspace.papi.service.context.RoutingServiceContext;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.service.threading.ThreadingServiceContext;
import com.rackspace.papi.service.threading.ThreadingService;

import javax.naming.Context;
import javax.naming.NamingException;

public class JndiContextAdapter implements ContextAdapter {

    private final Context namingContext;

    public JndiContextAdapter(Context namingContext) {
        this.namingContext = namingContext;
    }

    public static <T> T lookup(String name, Context context) throws ServiceUnavailableException {
        try {
            final Object o = context.lookup(name);
            return ((ServiceContext<T>) o).getService();
        } catch (NamingException ne) {
            throw new ServiceUnavailableException(name + " service is unavailable. Reason: " + ne.getExplanation(), ne.getCause());
        }
    }

    @Override
    public ConfigurationService configurationService() throws ServiceUnavailableException {
        return lookup(ConfigurationServiceContext.SERVICE_NAME, namingContext);
    }

   @Override
   public ClassLoaderManagerService classLoader() throws ServiceUnavailableException {
      return lookup(ClassLoaderServiceContext.SERVICE_NAME, namingContext);
   }

    @Override
    public DatastoreService datastoreService() throws ServiceUnavailableException {
        return lookup(DatastoreServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public ResponseMessageService responseMessageService() throws ServiceUnavailableException {
        return lookup(ResponseMessageServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public EventService eventService() throws ServiceUnavailableException {
        return lookup(EventManagerServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public ThreadingService threadingService() throws ServiceUnavailableException {
        return lookup(ThreadingServiceContext.SERVICE_NAME, namingContext);
    }
    
    @Override
    public RoutingService routingService() throws ServiceUnavailableException {
       return lookup(RoutingServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public GarbageCollectionService filterChainGarbageCollectorService() throws ServiceUnavailableException {
        return lookup(FilterChainGCServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public LoggingService loggingService() throws ServiceUnavailableException {
        return lookup(LoggingServiceContext.SERVICE_NAME, namingContext);
    }

   @Override
   public ContainerConfigurationService containerConfigurationService() throws ServiceUnavailableException {
        return lookup(ContainerServiceContext.SERVICE_NAME, namingContext);
   }
}
