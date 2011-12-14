package com.rackspace.papi.filter.resource;

import com.rackspace.papi.filter.PowerFilterChainBuilder;

/**
 *
 * @author zinic
 */
public class PowerFilterChainReclaimer {

   private final PowerFilterChainBuilder filterChainBuilder;

   public PowerFilterChainReclaimer(PowerFilterChainBuilder filterChainBuilder) {
      this.filterChainBuilder = filterChainBuilder;
   }

   public boolean passiveReclaim() {
      if (filterChainBuilder.getResourceConsumerMonitor().currentConsumerCount() != 0) {
         reclaim();
         return true;
      }

      return false;
   }

   public void reclaim() {
      filterChainBuilder.destroy();
   }
}
