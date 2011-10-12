package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;

/**
 *
 * @author Dan Daley
 */
public class LockableConfigurationListener<Listener extends UpdateListener<Config>, Config> {

   private final Listener listener;
   private final UpdateListener<Config> configurationListener;
   private final KeyedStackLock configurationLock;
   private final Object readKey, updateKey;

   public LockableConfigurationListener(Listener handler) {
      this.listener = handler;
      configurationLock = new KeyedStackLock();

      readKey = new Object();
      updateKey = new Object();

      configurationListener = new LockedConfigurationUpdater<Config>(configurationLock, updateKey) {

         @Override
         protected void onConfigurationUpdated(Config configurationObject) {
            LockableConfigurationListener.this.listener.configurationUpdated(configurationObject);
         }
      };
   }

   public UpdateListener<Config> getConfigurationListener() {
      return configurationListener;
   }

   protected void lockConfigurationForRead() {
      configurationLock.lock(readKey);
   }

   protected void unlockConfigurationForRead() {
      configurationLock.unlock(readKey);
   }

   protected void lockConfigurationForUpdate() {
      configurationLock.lock(updateKey);
   }

   protected void unlockConfigurationForUpdate() {
      configurationLock.unlock(updateKey);
   }

}
