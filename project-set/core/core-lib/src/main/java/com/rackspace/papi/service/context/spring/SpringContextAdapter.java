package com.rackspace.papi.service.context.spring;

import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
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
    public static final String REQUEST_PROXY_SERVICE_CONTEXT = "requestProxyServiceContext";
    public static final String REPORTING_SERVICE_CONTEXT = "reportingServiceContext";
    public static final String REQUEST_HEADER_SERVICE_CONTEXT = "requestHeaderServiceContext";
    public static final String RESPONSE_HEADER_SERVICE_CONTEXT = "responseHeaderServiceContext";

    private final ApplicationContext applicationContext;

    public SpringContextAdapter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ClassLoaderManagerService classLoader() {
        return classLoaderContext().getService();
    }

    public ServiceContext<ClassLoaderManagerService> classLoaderContext() {
        return (ServiceContext<ClassLoaderManagerService>) applicationContext.getBean(CLASS_LOADER_SERVICE_CONTEXT);
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
        return ((ServiceContext<ConfigurationService>) applicationContext.getBean(CONFIGURATION_SERVICE_CONTEXT)).getService();
    }

    @Override
    public ContainerConfigurationService containerConfigurationService() {
        return ((ServiceContext<ContainerConfigurationService>) applicationContext.getBean(CONTAINER_SERVICE_CONTEXT)).getService();
    }

    @Override
    public DatastoreService datastoreService() {
        return ((ServiceContext<DatastoreService>) applicationContext.getBean(DATASTORE_SERVICE_CONTEXT)).getService();
    }

    @Override
    public EventService eventService() {
        return ((ServiceContext<EventService>) applicationContext.getBean(EVENT_MANAGER_SERVICE_CONTEXT)).getService();
    }

    @Override
    public GarbageCollectionService filterChainGarbageCollectorService() {
        return ((ServiceContext<GarbageCollectionService>) applicationContext.getBean(FILTER_CHAIN_GC_SERVICE_CONTEXT)).getService();
    }

    @Override
    public LoggingService loggingService() {
        return ((ServiceContext<LoggingService>) applicationContext.getBean(LOGGING_SERVICE_CONTEXT)).getService();
    }

    @Override
    public ResponseMessageService responseMessageService() {
        return ((ServiceContext<ResponseMessageService>) applicationContext.getBean(RESPONSE_MESSAGE_SERVICE_CONTEXT)).getService();
    }

    @Override
    public RoutingService routingService() {
        return ((ServiceContext<RoutingService>) applicationContext.getBean(ROUTING_SERVICE_CONTEXT)).getService();
    }

    @Override
    public ThreadingService threadingService() {
        return ((ServiceContext<ThreadingService>) applicationContext.getBean(THREADING_SERVICE_CONTEXT)).getService();
    }

    @Override
    public RequestProxyService requestProxyService() {
        return ((ServiceContext<RequestProxyService>) applicationContext.getBean(REQUEST_PROXY_SERVICE_CONTEXT)).getService();
    }

    @Override
    public ReportingService reportingService() {
        return ((ServiceContext<ReportingService>) applicationContext.getBean(REPORTING_SERVICE_CONTEXT)).getService();
    }

    @Override
    public RequestHeaderService requestHeaderService() {
        return ((ServiceContext<RequestHeaderService>) applicationContext.getBean(REQUEST_HEADER_SERVICE_CONTEXT)).getService();
    }

    @Override
    public ResponseHeaderService responseHeaderService() {
        return ((ServiceContext<ResponseHeaderService>) applicationContext.getBean(RESPONSE_HEADER_SERVICE_CONTEXT)).getService();
    }
}
