package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.service.config.manager.ConfigurationUpdateManager;
import org.openrepose.core.service.config.manager.UpdateListener;
import org.openrepose.core.service.config.parser.ConfigurationParser;
import org.openrepose.core.service.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.commons.util.thread.Poller;
import com.rackspace.papi.jmx.ConfigurationInformation;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.threading.ThreadingService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named
public class ConfigurationUpdateManagerImpl implements ConfigurationUpdateManager {

    private final Map<String, Map<Integer, ParserListenerPair>> listenerMap;
    private final EventService eventManager;
    private final PowerApiUpdateManagerEventListener powerApiUpdateManagerEventListener;
    private final ThreadingService threadingService;
    private ConfigurationResourceWatcher resourceWatcher;
    private DestroyableThreadWrapper resrouceWatcherThread;
    private ConfigurationInformation configurationInformation;

    @Inject
    public ConfigurationUpdateManagerImpl(EventService eventManager,
                                          ThreadingService threadingService,
                                          ConfigurationInformation configurationInformation) {
        this.threadingService = threadingService;
        this.configurationInformation = configurationInformation;
        this.eventManager = eventManager;

        listenerMap = new HashMap<String, Map<Integer, ParserListenerPair>>();
        powerApiUpdateManagerEventListener = new PowerApiUpdateManagerEventListener(listenerMap);
    }

    @PostConstruct
    public void afterPropertiesSet() {
        // Initialize the resource watcher
        resourceWatcher = new ConfigurationResourceWatcher(eventManager);

        //TODO: Make this interval configurable
        final Poller pollerLogic = new Poller(resourceWatcher, 15000);

        resrouceWatcherThread = new DestroyableThreadWrapper(
                threadingService.newThread(pollerLogic, "Configuration Watcher Thread"), pollerLogic);
        resrouceWatcherThread.start();

        // Listen for configuration events
        eventManager.listen(powerApiUpdateManagerEventListener, ConfigurationEvent.class);
    }

    public PowerApiUpdateManagerEventListener getPowerApiUpdateManagerEventListener() {
        return powerApiUpdateManagerEventListener;
    }

    @PreDestroy
    public synchronized void destroy() {
        resrouceWatcherThread.destroy();
        listenerMap.clear();
    }

    @Override
    public synchronized <T> void registerListener(UpdateListener<T> listener, ConfigurationResource resource, ConfigurationParser<T> parser, String filterName) {
        Map<Integer, ParserListenerPair> resourceListeners = listenerMap.get(resource.name());

        if (resourceListeners == null) {
            resourceListeners = new HashMap<Integer, ParserListenerPair>();

            listenerMap.put(resource.name(), resourceListeners);
            resourceWatcher.watch(resource);
        }

        resourceListeners.put(listener.hashCode(), new ParserListenerPair(listener, parser, this.configurationInformation, filterName));
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
