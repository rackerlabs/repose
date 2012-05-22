package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.thread.RecurringTask;
import com.rackspace.papi.service.event.common.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
 *
 */
public class ConfigurationResourceWatcher implements RecurringTask {

   private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResourceWatcher.class);
   private final Map<String, ConfigurationResource> watchMap;
   private final EventService eventManager;
   
   public ConfigurationResourceWatcher(EventService eventManager) {
      this.eventManager = eventManager;
      watchMap = new HashMap<String, ConfigurationResource>();
   }

   @Override
   public synchronized void run() {
      for (ConfigurationResource resource : watchMap.values()) {
         try {
            if (resource.updated()) {
               eventManager.newEvent(ConfigurationEvent.UPDATE, resource);
               LOG.info("Updated " + resource.name());
            }
         } catch (Exception e) {
            // TODO:Log - Create a logger that is smart enough not to print out errors we don't care about more than once
         }
      }
   }

   public synchronized void watch(ConfigurationResource resource) {
      watchMap.put(resource.name(), resource);
   }

   public synchronized void stopWatching(String resourceName) {
      watchMap.remove(resourceName);
   }
}
