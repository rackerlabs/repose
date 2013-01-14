package com.rackspace.papi.service.context.jndi;

import com.rackspace.papi.filter.PowerFilterChainBuilder;
import com.rackspace.papi.service.ServiceUnavailableException;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.context.impl.*;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import com.rackspace.papi.service.headers.request.RequestHeaderService;
import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.logging.LoggingService;
import com.rackspace.papi.service.proxy.RequestProxyService;
import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.service.threading.ThreadingService;
import com.rackspace.papi.service.threading.impl.ThreadingServiceContext;

import javax.naming.Context;
import javax.naming.NamingException;

public class JndiContextAdapter implements ContextAdapter {
    private static final String NOT_SUPPORTED = "Not supported yet.";
    private final Context namingContext;

    public JndiContextAdapter(Context namingContext) {
        this.namingContext = namingContext;
    }

    public <T> T lookup(String name) {
        try {
            final Object o = namingContext.lookup(name);
            return ((ServiceContext<T>) o).getService();
        } catch (NamingException ne) {
            throw new ServiceUnavailableException(name + " service is unavailable. Reason: " + ne.getExplanation(), ne.getCause());
        }
    }

    @Override
    public ConfigurationService configurationService() {
        return lookup(ConfigurationServiceContext.SERVICE_NAME);
    }

    @Override
    public ClassLoaderManagerService classLoader() {
        return lookup(ClassLoaderServiceContext.SERVICE_NAME);
    }

    @Override
    public DatastoreService datastoreService() {
        return lookup(DatastoreServiceContext.SERVICE_NAME);
    }

    @Override
    public ResponseMessageService responseMessageService() {
        return lookup(ResponseMessageServiceContext.SERVICE_NAME);
    }

    @Override
    public EventService eventService() {
        return lookup(EventManagerServiceContext.SERVICE_NAME);
    }

    @Override
    public ThreadingService threadingService() {
        return lookup(ThreadingServiceContext.SERVICE_NAME);
    }

    @Override
    public RoutingService routingService() {
        return lookup(RoutingServiceContext.SERVICE_NAME);
    }

    @Override
    public GarbageCollectionService filterChainGarbageCollectorService() {
        return lookup(FilterChainGCServiceContext.SERVICE_NAME);
    }

    @Override
    public LoggingService loggingService() {
        return lookup(LoggingServiceContext.SERVICE_NAME);
    }

    @Override
    public ContainerConfigurationService containerConfigurationService() {
        return lookup(ContainerServiceContext.SERVICE_NAME);
    }

    @Override
    public ReportingService reportingService() {
        return lookup(ReportingServiceContext.SERVICE_NAME);
    }

    @Override
    public <T extends ServiceContext<?>> T getContext(Class<T> clazz) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public RequestProxyService requestProxyService() {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public String getReposeVersion() {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public RequestHeaderService requestHeaderService() {
        return lookup(RequestHeaderServiceContext.SERVICE_NAME);
    }

    @Override
    public ResponseHeaderService responseHeaderService() {
        return lookup(ResponseHeaderServiceContext.SERVICE_NAME);
    }

    @Override
    public PowerFilterChainBuilder filterChainBuilder() {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }
}
