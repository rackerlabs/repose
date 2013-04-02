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
        final String payloadName = e.payload().name();
        Map<Integer, ParserListenerPair> listeners = getListenerMap(payloadName);
        
        LOG.info("Configuration event triggered for: " + payloadName);
        LOG.info("Notifying " + listeners.values().size() + " listeners");

        for (ParserListenerPair parserListener : listeners.values()) {
            UpdateListener updateListener = parserListener.getListener();
           
            if (updateListener != null) {
                LOG.info("Notifying " + updateListener.getClass().getName());
                
                currentThread.setContextClassLoader(parserListener.getClassLoader());
                try {
                    configUpdate(updateListener, parserListener.getParser().read(e.payload()));
                   if(parserListener.getFilterName()!=null && !parserListener.getFilterName().isEmpty() && updateListener.isInitialized() ){
                    parserListener.getConfigurationInformation().setFilterLoadingInformation(parserListener.getFilterName(),updateListener.isInitialized(), e.payload());
                   }else{
                       parserListener.getConfigurationInformation().setFilterLoadingFailedInformation(parserListener.getFilterName(), e.payload(), "Failed loading File"); 
                   }
                }catch(Exception ex){
                    
                   if(parserListener.getFilterName()!=null && !parserListener.getFilterName().isEmpty()){
                    parserListener.getConfigurationInformation().setFilterLoadingFailedInformation(parserListener.getFilterName(), e.payload(), ex.getMessage()); 
                   }
                   LOG.error("Configuration update error. Reason: " + ex.getMessage(), ex);
                   
                } finally {
                    currentThread.setContextClassLoader(previousClassLoader);
                }
            } else {
                LOG.warn("Update listener is null for " + payloadName);
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
