package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import java.util.*;

/**
 *
 * @author Dan Daley
 */
public abstract class AbstractConfiguredFilterHandlerFactory<Handler extends FilterLogicHandler> implements UpdateListener {

   private final Map<Class, UpdateListener<?>> listeners;
   private final KeyedStackLock configurationLock;
   private final Object readKey, updateKey;

   public AbstractConfiguredFilterHandlerFactory() {
      configurationLock = new KeyedStackLock();

      readKey = new Object();
      updateKey = new Object();
      listeners = getListeners();
   }

   protected abstract Handler buildHandler();

   protected abstract Map<Class, UpdateListener<?>> getListeners();

   public Handler newHandler() {
      configurationLock.lock(readKey);
      try {
         return buildHandler();
      } finally {
         configurationLock.unlock(readKey);
      }
   }

   public UpdateListener getListener(Class configClass) {
      return listeners.get(configClass);
   }

   @Override
   public void configurationUpdated(Object configurationObject) {
      UpdateListener listener = listeners.get(configurationObject.getClass());
      if (listener != null) {

         configurationLock.lock(updateKey);
         try {
            listener.configurationUpdated(configurationObject);
         } finally {
            configurationLock.unlock(updateKey);
         }

      }

   }
;
}
