package com.rackspace.papi.filter.resource;

import com.rackspace.papi.filter.PowerFilterChainBuilder;
import com.rackspace.papi.service.filterchain.FilterChainGarbageCollectorService;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author zinic
 */
public class PowerFilterChainGarbageCollector implements FilterChainGarbageCollectorService {

   private final List<PowerFilterChainReclaimer> gcList;

   public PowerFilterChainGarbageCollector() {
      gcList = new LinkedList<PowerFilterChainReclaimer>();
   }

   @Override
   public synchronized void retireFilterChainBuilder(PowerFilterChainBuilder filterChainBuilder) {
      gcList.add(new PowerFilterChainReclaimer(filterChainBuilder));
   }

   private synchronized List<PowerFilterChainReclaimer> getGarbageCollectors() {
      return new LinkedList<PowerFilterChainReclaimer>(gcList);
   }

   public void sweepGarbageCollectors() {
      for (PowerFilterChainReclaimer gc : getGarbageCollectors()) {
         gc.passiveReclaim();
      }
   }
}
