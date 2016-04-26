/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.config.impl;

import org.openrepose.commons.config.manager.ConfigurationUpdateManager;
import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.commons.utils.thread.Poller;
import org.openrepose.core.services.event.Event;
import org.openrepose.core.services.event.EventListener;
import org.openrepose.core.services.event.EventService;
import org.openrepose.core.services.threading.ThreadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
public class ConfigurationUpdateManagerImpl implements ConfigurationUpdateManager, EventListener<ConfigurationEvent, ConfigurationResource> {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ParserListenerPair>> listenerMap;
    private final Object listenerLock = new Object(); //AUGH
    private final EventService eventManager;
    private final ThreadingService threadingService;
    private ConfigurationResourceWatcher resourceWatcher;
    private DestroyableThreadWrapper resourceWatcherThread;

    @Inject
    public ConfigurationUpdateManagerImpl(
            EventService eventManager,
            ThreadingService threadingService
    ) {
        this.eventManager = eventManager;
        this.threadingService = threadingService;

        listenerMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void initialize() {
        // Initialize the resource watcher
        resourceWatcher = new ConfigurationResourceWatcher(eventManager);

        //TODO: Make this interval configurable
        final Poller pollerLogic = new Poller(resourceWatcher, 15000);

        resourceWatcherThread = new DestroyableThreadWrapper(
                threadingService.newThread(pollerLogic, "Configuration Watcher Thread"), pollerLogic);
        resourceWatcherThread.start();

        // Listen for configuration events
        eventManager.listen(this, ConfigurationEvent.class);
    }

    @PreDestroy
    @Override
    public void destroy() {
        resourceWatcherThread.destroy();
        listenerMap.clear();
    }

    @Override
    public <T> void registerListener(UpdateListener<T> listener, ConfigurationResource resource, ConfigurationParser<T> parser, String filterName) {
        synchronized (listenerLock) {
            ConcurrentHashMap<Integer, ParserListenerPair> resourceListeners = listenerMap.get(resource.name());

            if (resourceListeners == null) {
                resourceListeners = new ConcurrentHashMap<>();

                listenerMap.put(resource.name(), resourceListeners);
                resourceWatcher.watch(resource);
            }

            resourceListeners.put(listener.hashCode(), new ParserListenerPair(listener, parser, filterName));
        }
    }

    @Override
    public <T> void unregisterListener(UpdateListener<T> listener, ConfigurationResource resource) {
        synchronized (listenerLock) {
            ConcurrentHashMap<Integer, ParserListenerPair> resourceListeners = listenerMap.get(resource.name());

            if (resourceListeners != null) {
                resourceListeners.remove(listener.hashCode());

                if (resourceListeners.isEmpty()) {
                    resourceWatcher.stopWatching(resource.name());
                    listenerMap.remove(resource.name());
                }
            }
        }
    }


    @Override
    public void onEvent(Event<ConfigurationEvent, ConfigurationResource> e) {
        final String payloadName = e.payload().name();
        Map<Integer, ParserListenerPair> listeners = listenerMap.get(payloadName);

        LOG.info("Configuration event triggered for: " + payloadName);
        LOG.info("Notifying " + listeners.values().size() + " listeners");
        for (ParserListenerPair parserListener : listeners.values()) {
            UpdateListener updateListener = parserListener.getListener();

            if (updateListener != null) {
                LOG.info("Notifying " + updateListener.getClass().getName());

                try {
                    configUpdate(updateListener, parserListener.getParser().read(e.payload()));
                } catch (Exception ex) {
                    LOG.error("Configuration update error. Reason: {}", ex.getLocalizedMessage());
                    LOG.trace("", ex);
                }
            } else {
                LOG.warn("Update listener is null for " + payloadName);
            }
        }
    }

    private void configUpdate(UpdateListener upd, Object cfg) throws UpdateFailedException {
        upd.configurationUpdated(cfg);
        LOG.debug("Configuration Updated: " + cfg.toString());

    }

}
