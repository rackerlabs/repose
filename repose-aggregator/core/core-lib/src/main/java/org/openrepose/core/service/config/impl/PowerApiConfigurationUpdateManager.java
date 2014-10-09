package org.openrepose.core.service.config.impl;

import org.openrepose.commons.config.manager.ConfigurationUpdateManager;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.commons.utils.thread.Poller;
import org.openrepose.core.jmx.ConfigurationInformation;
import org.openrepose.core.service.context.ServletContextHelper;
import org.openrepose.core.service.event.common.EventService;
import org.openrepose.core.service.threading.ThreadingService;

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
