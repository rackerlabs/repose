package com.rackspace.papi.filter.resource.reclaim;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.filter.resource.ResourceMonitor;

/**
 *
 * @author zinic
 */
public class ResourceUseConstrainedReclaimer implements ObjectReclaimer {

   private final Destroyable reclaimableObject;
   private final ResourceMonitor resourceMonitor;

   public ResourceUseConstrainedReclaimer(Destroyable reclaimableObject, ResourceMonitor resourceMonitor) {
      this.reclaimableObject = reclaimableObject;
      this.resourceMonitor = resourceMonitor;
   }

   @Override
   public boolean passiveReclaim() {
      final boolean resourceInUse = resourceMonitor.inUse();
      
      if (!resourceInUse) {
         reclaim();
      }

      return !resourceInUse;
   }

   @Override
   public void reclaim() {
      reclaimableObject.destroy();
   }
}
