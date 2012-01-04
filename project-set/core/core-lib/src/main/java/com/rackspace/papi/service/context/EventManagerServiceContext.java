package com.rackspace.papi.service.context;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.event.EventService;
import com.rackspace.papi.service.event.PowerProxyEventKernel;
import com.rackspace.papi.service.event.PowerProxyEventManager;
import com.rackspace.papi.service.threading.ThreadingService;
import javax.servlet.ServletContextEvent;

public class EventManagerServiceContext implements ServiceContext<EventService> {

    public static final String SERVICE_NAME = "powerapi:/kernel/event";
    
    private final EventService eventManager;
    private DestroyableThreadWrapper eventKernelThread;

    public EventManagerServiceContext() {
        eventManager = new PowerProxyEventManager();
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
        final ThreadingService threadManager = ServletContextHelper.getPowerApiContext(sce.getServletContext()).threadingService();

        final PowerProxyEventKernel eventKernel = new PowerProxyEventKernel(eventManager);
        eventKernelThread = new DestroyableThreadWrapper(threadManager.newThread(eventKernel, "Event Kernel Thread"), eventKernel);
        
        eventKernelThread.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        eventKernelThread.destroy();
    }
}
