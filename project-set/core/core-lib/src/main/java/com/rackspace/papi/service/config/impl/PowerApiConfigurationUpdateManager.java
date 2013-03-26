package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.commons.util.thread.Poller;
import com.rackspace.papi.jmx.ConfigurationInformation;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.threading.ThreadingService;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

public class PowerApiConfigurationUpdateManager implements ConfigurationUpdateManager {

   private final Map<String, Map<Integer, ParserListenerPair>> listenerMap;
   private final EventService eventManager;
   private final PowerApiUpdateManagerEventListener powerApiUpdateManagerEventListener;
   private ConfigurationResourceWatcher resourceWatcher;
   private DestroyableThreadWrapper resrouceWatcherThread;
   private ConfigurationInformation configurationInformation;

   public PowerApiConfigurationUpdateManager(EventService eventManager) {
      this.eventManager = eventManager;

      listenerMap = new HashMap<String, Map<Integer, ParserListenerPair>>();
      powerApiUpdateManagerEventListener = new PowerApiUpdateManagerEventListener(listenerMap);
   }

   public void initialize(ServletContext ctx) {
      final ThreadingService threadingService = ServletContextHelper.getInstance(ctx).getPowerApiContext().threadingService();
      
      configurationInformation =(ConfigurationInformation)ServletContextHelper.getInstance(ctx).getPowerApiContext().reposeConfigurationInformation();
      // Initialize the resource watcher
      resourceWatcher = new ConfigurationResourceWatcher(eventManager);
      
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

   @Override
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

      resourceListeners.put(listener.hashCode(), new ParserListenerPair(listener, parser,this.configurationInformation,filterName));
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
