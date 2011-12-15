package com.rackspace.papi.filter.resource.reclaim;

/**
 *
 * @author zinic
 */
public interface ObjectReclaimer {

   boolean passiveReclaim();

   void reclaim();
}
