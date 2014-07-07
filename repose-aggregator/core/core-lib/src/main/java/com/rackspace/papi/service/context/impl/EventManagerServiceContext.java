package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.event.PowerProxyEventKernel;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.threading.ThreadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("eventManagerServiceContext")
public class EventManagerServiceContext implements ServiceContext<EventService> {

    public static final String SERVICE_NAME = "powerapi:/kernel/event";
    
    private final EventService eventManager;
    private DestroyableThreadWrapper eventKernelThread;
    private final ServiceRegistry registry;
    private final ThreadingService threadingService;
    private final PowerProxyEventKernel eventKernel;

    @Autowired
    public EventManagerServiceContext(
            @Qualifier("eventManager") EventService eventManager,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            ThreadingService threadingService,
            @Qualifier("powerProxyEventKernel") PowerProxyEventKernel eventKernel) {
       this.eventManager = eventManager;
       this.registry = registry;
       this.threadingService = threadingService;
       this.eventKernel = eventKernel;
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public EventService getService() {
        return eventManager;
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        eventKernelThread = new DestroyableThreadWrapper(threadingService.newThread(eventKernel, "Event Kernel Thread"), eventKernel);
        eventKernelThread.start();
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        eventKernelThread.destroy();
    }
}
