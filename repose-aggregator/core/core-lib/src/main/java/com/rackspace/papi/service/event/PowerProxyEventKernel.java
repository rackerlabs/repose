package com.rackspace.papi.service.event;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import org.openrepose.core.service.event.EventDispatcher;
import org.openrepose.core.service.event.EventService;
import com.rackspace.papi.service.threading.ThreadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

@Named("powerProxyEventKernel")
public class PowerProxyEventKernel implements Runnable, Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(PowerProxyEventKernel.class);
    private final EventService eventManager;
    private final ThreadingService threadingService;
    private volatile boolean shouldContinue;
    private DestroyableThreadWrapper eventKernelThread;

    @Inject
    public PowerProxyEventKernel( EventService eventManager, ThreadingService threadingService) {
        this.eventManager = eventManager;
        this.threadingService = threadingService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        eventKernelThread = new DestroyableThreadWrapper(threadingService.newThread(this, "Event Kernel Thread"), this);
        eventKernelThread.start();
    }

    @PreDestroy
    public void preDestroy() {
        eventKernelThread.destroy();
    }

    /**
     * Stupid destroyable needs to die in a fire
     */
    public void destroy() {
        shouldContinue = false;
    }

    @Override
    public void run() {
        shouldContinue = true;

        try {
            while (shouldContinue) {
                final EventDispatcher dispatcher = eventManager.nextDispatcher();
                
                if (LOG.isDebugEnabled()) {
                    final Enum eventType = dispatcher.getEvent().type();
                    
                    LOG.debug("Dispatching event: " + eventType.getClass().getSimpleName() + "." + eventType.name());
                }
                
                try {
                    dispatcher.dispatch();
                } catch (Exception ex) {
                    LOG.error("Exception caught while dispatching event, \""
                            + dispatcher.getEvent().type().getClass().getSimpleName() + "$" + dispatcher.getEvent().type().name()
                            + "\" - Reason: " + ex.getMessage(), ex);
                }
            }
        } catch (InterruptedException ie) {
            LOG.warn("Event kernel received an interrupt. Exiting event kernel loop.", ie);
            shouldContinue = false;

            Thread.currentThread().interrupt();
        }
    }

}
