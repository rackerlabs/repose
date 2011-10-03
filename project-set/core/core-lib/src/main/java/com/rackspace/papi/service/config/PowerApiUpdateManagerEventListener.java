package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.service.event.Event;
import com.rackspace.papi.service.event.listener.EventListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fran
 */
public class PowerApiUpdateManagerEventListener implements EventListener<ConfigurationEvent, ConfigurationResource> {
    private final Map<String, Map<Integer, ParserListenerPair>> listenerMap;

    public PowerApiUpdateManagerEventListener(Map<String, Map<Integer, ParserListenerPair>> listenerMap) {
        this.listenerMap = listenerMap;
    }

    @Override
    public void onEvent(Event<ConfigurationEvent, ConfigurationResource> e) {
        for (ParserListenerPair parserListener : getListenerMap(e.payload().name()).values()) {
            UpdateListener updateListener = parserListener.getListener();

            if (updateListener != null) {
                updateListener.configurationUpdated(parserListener.getParser().read(e.payload()));
            }
        }
    }

    public synchronized Map<Integer, ParserListenerPair> getListenerMap(String resourceName) {
        final Map<Integer, ParserListenerPair> mapReference = new HashMap<Integer, ParserListenerPair>(listenerMap.get(resourceName));

        return Collections.unmodifiableMap(mapReference != null ? mapReference : Collections.EMPTY_MAP);
    }
}
