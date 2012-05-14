package com.rackspace.papi.service.filterchain;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.filter.resource.ResourceMonitor;

public interface GarbageCollectionService {

   void reclaimDestroyable(Destroyable destroyable);
   void reclaimDestroyable(Destroyable destroyable, ResourceMonitor resourceMonitor);
   void sweepGarbageCollectors();
}
