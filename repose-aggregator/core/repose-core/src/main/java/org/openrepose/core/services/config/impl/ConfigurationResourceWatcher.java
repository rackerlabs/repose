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

import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.core.services.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a thread that continuously monitors configuration
 * resources that have been registered with PAPI via the
 * ConfigurationManager.subscribeTo call. The thread continues to run even when
 * Throwable is caught. Exceptions can be thrown if services have subscribed to
 * resource updates but the resource doesn't exist in the resources directory.
 * However, one of the requirements of PAPI is that a user can drop a resource
 * into the resources directory and PAPI will begin processing it on the fly. So
 * these "standard" exceptions will be caught but will allow the normal thread
 * processing to continue.
 */
public class ConfigurationResourceWatcher implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResourceWatcher.class);
    private final ConcurrentHashMap<String, ConfigurationResource> watchMap;
    private final EventService eventManager;

    public ConfigurationResourceWatcher(EventService eventManager) {
        this.eventManager = eventManager;
        watchMap = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        for (ConfigurationResource resource : watchMap.values()) {
            try {
                if (resource.updated()) {
                    eventManager.newEvent(ConfigurationEvent.UPDATE, resource);
                    LOG.info("Updated " + resource.name());
                }
            } catch (Exception e) {
                /**
                 * TODO:Log - Create a logger that is smart enough not to print out errors we don't care about more than once
                 * Trace logging the exception, because the context knowledge of this exception has been lost. We don't know
                 * for certain that we can catch the IOException, because we might be relying on this to catch runtime
                 * exceptions. Bad bad bad.
                 */
                LOG.trace("Error updating resource: " + resource.name(), e);
            }
        }
    }

    public void watch(ConfigurationResource resource) {
        watchMap.put(resource.name(), resource);
    }

    public void stopWatching(String resourceName) {
        watchMap.remove(resourceName);
    }
}
