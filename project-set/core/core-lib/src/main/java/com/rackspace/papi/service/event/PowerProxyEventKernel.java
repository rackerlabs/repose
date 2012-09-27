package com.rackspace.papi.service.event;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.event.common.EventDispatcher;
import com.rackspace.papi.service.event.common.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("powerProxyEventKernel")
public class PowerProxyEventKernel implements Runnable, Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(PowerProxyEventKernel.class);
    private final EventService eventManager;
    private volatile boolean shouldContinue;

    @Autowired
    public PowerProxyEventKernel(@Qualifier("eventManager") EventService eventManager) {
        this.eventManager = eventManager;
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
            LOG.warn("Event kernel received an interrupt. Exiting event kernel loop.");
            shouldContinue = false;

            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() {
        shouldContinue = false;
    }
}
