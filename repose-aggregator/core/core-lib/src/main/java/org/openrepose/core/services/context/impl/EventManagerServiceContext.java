package org.openrepose.core.services.context.impl;

import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.core.services.event.PowerProxyEventKernel;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.threading.impl.ThreadingServiceImpl;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("eventManagerServiceContext")
public class EventManagerServiceContext implements ServiceContext<EventService> {

    public static final String SERVICE_NAME = "powerapi:/kernel/event";
    
    private final EventService eventManager;
    private DestroyableThreadWrapper eventKernelThread;
    private final ServiceRegistry registry;
    private final ThreadingServiceImpl threadingService;
    private final PowerProxyEventKernel eventKernel;

    @Autowired
    public EventManagerServiceContext(
            @Qualifier("eventManager") EventService eventManager,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("threadingService") ThreadingServiceImpl threadingService,
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
