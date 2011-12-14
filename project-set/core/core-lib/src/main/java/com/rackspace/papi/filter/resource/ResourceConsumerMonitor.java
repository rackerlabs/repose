package com.rackspace.papi.filter.resource;

/**
 *
 * @author zinic
 */
public class ResourceConsumerMonitor implements ResourceMonitor {

   private int resourceConsumers;

   public ResourceConsumerMonitor() {
      resourceConsumers = 0;
   }

   public int currentConsumerCount() {
      return resourceConsumers;
   }

   @Override
   public void inUse() {
      resourceConsumers++;
   }

   @Override
   public void released() {
      resourceConsumers--;
   }
}
