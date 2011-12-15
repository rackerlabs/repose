package com.rackspace.papi.filter.resource;

/**
 *
 * @author zinic
 */
public interface ResourceMonitor {

   boolean inUse();
   
   void use();

   void released();
}
