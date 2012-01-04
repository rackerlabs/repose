package com.rackspace.papi.service.filterchain;

import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.filter.resource.reclaim.ResourceUseConstrainedReclaimer;
import com.rackspace.papi.filter.resource.reclaim.UnconstrainedReclaimer;
import com.rackspace.papi.filter.resource.reclaim.ObjectReclaimer;
import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.filterchain.GarbageCollectionService;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author zinic
 */
public class SweepingGarbageCollector implements GarbageCollectionService {

   private final List<ObjectReclaimer> reclaimerList;

   public SweepingGarbageCollector() {
      reclaimerList = new LinkedList<ObjectReclaimer>();
   }

   @Override
   public synchronized void reclaimDestroyable(Destroyable destroyable) {
      reclaimerList.add(new UnconstrainedReclaimer(destroyable));
   }

   @Override
   public synchronized void reclaimDestroyable(Destroyable destroyable, ResourceMonitor resourceMonitor) {
      reclaimerList.add(new ResourceUseConstrainedReclaimer(destroyable, resourceMonitor));
   }

   public synchronized void reclamationComplete(ObjectReclaimer or) {
      reclaimerList.remove(or);
   }

   public synchronized List<ObjectReclaimer> getGarbageCollectorsCopy() {
      return new LinkedList<ObjectReclaimer>(reclaimerList);
   }

   public void sweepGarbageCollectors() {
      for (ObjectReclaimer objectReclaimer : getGarbageCollectorsCopy()) {
         if (objectReclaimer.passiveReclaim()) {
            reclamationComplete(objectReclaimer);
         }
      }
   }
}
