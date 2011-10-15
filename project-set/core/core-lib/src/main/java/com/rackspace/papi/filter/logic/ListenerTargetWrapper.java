package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;

/**
 *
 * @author Dan Daley
 */
public class ListenerTargetWrapper<Target> implements UpdateListener<Target>
{
   private final UpdateListener<Target> targetListener;
   private final KeyedStackLock configurationLock;
   private final Object updateKey;
   
   public ListenerTargetWrapper(UpdateListener<Target> targetListener, KeyedStackLock configurationLock, Object updateKey) {
      this.configurationLock = configurationLock;
      this.updateKey = updateKey;
      this.targetListener = targetListener;
   }
   
   @Override
   public void configurationUpdated(Target configurationObject) {
      configurationLock.lock(updateKey);
      try {
         targetListener.configurationUpdated(configurationObject);
      } finally {
         configurationLock.unlock(updateKey);
      }
   }
   
}
