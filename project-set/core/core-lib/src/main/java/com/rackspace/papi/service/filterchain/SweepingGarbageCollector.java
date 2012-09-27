package com.rackspace.papi.service.filterchain;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.filter.resource.reclaim.ObjectReclaimer;
import com.rackspace.papi.filter.resource.reclaim.ResourceUseConstrainedReclaimer;
import com.rackspace.papi.filter.resource.reclaim.UnconstrainedReclaimer;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author zinic
 */
@Component("garbageService")
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

   @Override
   public void sweepGarbageCollectors() {
      for (ObjectReclaimer objectReclaimer : getGarbageCollectorsCopy()) {
         if (objectReclaimer.passiveReclaim()) {
            reclamationComplete(objectReclaimer);
         }
      }
   }
}
