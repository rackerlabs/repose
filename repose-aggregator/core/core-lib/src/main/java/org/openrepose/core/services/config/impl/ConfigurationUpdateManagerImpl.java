package org.openrepose.core.services.config.impl;

import org.openrepose.commons.config.manager.ConfigurationUpdateManager;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.commons.utils.thread.Poller;
import org.openrepose.nodeservice.jmx.ConfigurationInformation;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.threading.ThreadingService;

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
    private ConfigurationResourceWatcher resourceWatcher;
    private DestroyableThreadWrapper resourceWatcherThread;
    private final ConfigurationInformation configurationInformation;
    private final ThreadingService threadingService;

    @Inject
    public ConfigurationUpdateManagerImpl(
            EventService eventManager,
            ConfigurationInformation configurationInformation,
            ThreadingService threadingService
    ) {
        this.eventManager = eventManager;
        this.configurationInformation = configurationInformation;
        this.threadingService = threadingService;

        listenerMap = new HashMap<>();
        powerApiUpdateManagerEventListener = new PowerApiUpdateManagerEventListener(listenerMap);
    }

    public void initialize() {
        // Initialize the resource watcher
        resourceWatcher = new ConfigurationResourceWatcher(eventManager);

        //TODO: Make this interval configurable
        final Poller pollerLogic = new Poller(resourceWatcher, 15000);

        resourceWatcherThread = new DestroyableThreadWrapper(
                threadingService.newThread(pollerLogic, "Configuration Watcher Thread"), pollerLogic);
        resourceWatcherThread.start();

        // Listen for configuration events
        eventManager.listen(powerApiUpdateManagerEventListener, ConfigurationEvent.class);
    }

    @PreDestroy
    @Override
    public synchronized void destroy() {
        resourceWatcherThread.destroy();
        listenerMap.clear();
    }

    @Override
    public synchronized <T> void registerListener(UpdateListener<T> listener, ConfigurationResource resource, ConfigurationParser<T> parser, String filterName) {
        Map<Integer, ParserListenerPair> resourceListeners = listenerMap.get(resource.name());

        if (resourceListeners == null) {
            resourceListeners = new HashMap<>();

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
