package com.rackspace.papi.filter.resource;

/**
 *
 * @author zinic
 */
public interface ResourceMonitor {

   void inUse();

   void released();
}
