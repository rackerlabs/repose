package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.ConfigurationObjectParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.service.event.EventService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;

public class PowerApiConfigurationUpdateManager implements ConfigurationUpdateManager {

    private final Map<String, Map<Integer, ParserListenerPair>> listenerMap;
    private final EventService eventManager;
    private final PowerApiUpdateManagerEventListener powerApiUpdateManagerEventListener;
    
    private ConfigurationResourceWatcher resourceWatcher;
    private DestroyableThreadWrapper configurationResourceWatcherThread;

    public PowerApiConfigurationUpdateManager(EventService eventManager) {
        this.eventManager = eventManager;
        
        listenerMap = new HashMap<String, Map<Integer, ParserListenerPair>>();
        powerApiUpdateManagerEventListener = new PowerApiUpdateManagerEventListener(listenerMap);
    }

    public void initialize(ServletContext ctx) {
        eventManager.listen(powerApiUpdateManagerEventListener, ConfigurationEvent.class);

        resourceWatcher = new ConfigurationResourceWatcher(eventManager);
        configurationResourceWatcherThread = new DestroyableThreadWrapper(
                ServletContextHelper.getPowerApiContext(ctx).threadingService().newThread(resourceWatcher, "Configuration Watcher Thread"), resourceWatcher);

        configurationResourceWatcherThread.start();
    }

    public PowerApiUpdateManagerEventListener getPowerApiUpdateManagerEventListener() {
        return powerApiUpdateManagerEventListener;
    }

    @Override
    public synchronized void destroy() {
        configurationResourceWatcherThread.destroy();
        listenerMap.clear();
    }

    @Override
    public synchronized <T> void registerListener(UpdateListener<T> listener, ConfigurationResource resource, ConfigurationObjectParser<T> parser) {
        Map<Integer, ParserListenerPair> resourceListeners = listenerMap.get(resource.name());

        if (resourceListeners == null) {
            resourceListeners = new HashMap<Integer, ParserListenerPair>();
            
            listenerMap.put(resource.name(), resourceListeners);
            resourceWatcher.watch(resource);
        }

        resourceListeners.put(listener.hashCode(), new ParserListenerPair(listener, parser));
    }

    @Override
    public synchronized <T> void unregisterListener(UpdateListener<T> listener, ConfigurationResource resource) {
        Map<Integer, ParserListenerPair> resourceListeners = listenerMap.get(resource.name());

        if (resourceListeners != null) {
            resourceListeners.remove(listener.hashCode());
            
            if (resourceListeners.isEmpty()) {
                resourceWatcher.stopWatching(resource.name());
                listenerMap.remove(resource.name());
            }
        }
    }
}
