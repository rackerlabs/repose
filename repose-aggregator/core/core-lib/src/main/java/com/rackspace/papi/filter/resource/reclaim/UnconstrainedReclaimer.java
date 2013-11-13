package com.rackspace.papi.filter.resource.reclaim;

import com.rackspace.papi.commons.util.Destroyable;

/**
 *
 * @author zinic
 */
public class UnconstrainedReclaimer implements ObjectReclaimer {

   private final Destroyable destroyable;

   public UnconstrainedReclaimer(Destroyable destroyable) {
      this.destroyable = destroyable;
   }

   @Override
   public boolean passiveReclaim() {
      reclaim();

      return true;
   }

   @Override
   public void reclaim() {
      destroyable.destroy();
   }
}
