package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fran
 */
public class PowerApiUpdateManagerEventListener implements EventListener<ConfigurationEvent, ConfigurationResource> {

    private final Map<String, Map<Integer, ParserListenerPair>> listenerMap;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(PowerApiUpdateManagerEventListener.class);

    public PowerApiUpdateManagerEventListener(Map<String, Map<Integer, ParserListenerPair>> listenerMap) {
        this.listenerMap = listenerMap;
    }

    @Override
    public void onEvent(Event<ConfigurationEvent, ConfigurationResource> e) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();

        for (ParserListenerPair parserListener : getListenerMap(e.payload().name()).values()) {
            UpdateListener updateListener = parserListener.getListener();

            if (updateListener != null) {
                currentThread.setContextClassLoader(parserListener.getClassLoader());
                try {
                    configUpdate(updateListener, parserListener.getParser().read(e.payload()));
                } finally {
                    currentThread.setContextClassLoader(previousClassLoader);
                }
            }
        }
    }

    public synchronized Map<Integer, ParserListenerPair> getListenerMap(String resourceName) {
        final Map<Integer, ParserListenerPair> mapReference = new HashMap<Integer, ParserListenerPair>(listenerMap.get(resourceName));

        return Collections.unmodifiableMap(mapReference);
    }

    private void configUpdate(UpdateListener upd, Object cfg) {
        upd.configurationUpdated(cfg);
        LOG.debug("Configuration Updated:\n" + cfg.toString());

    }
}
