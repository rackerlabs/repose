package com.rackspace.papi.filter.resource;

/**
 *
 * @author zinic
 */
public class ResourceConsumerCounter implements ResourceMonitor {

   private int resourceConsumers;

   public ResourceConsumerCounter() {
      resourceConsumers = 0;
   }

   @Override
   public boolean inUse() {
      return resourceConsumers > 0;
   }

   @Override
   public void use() {
      resourceConsumers++;
   }

   @Override
   public void released() {
      resourceConsumers--;
   }
}
