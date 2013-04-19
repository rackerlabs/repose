/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.routing.RoutingService;


public interface DistributedDatastoreLauncherService extends Destroyable{
   
   public void initialize(ConfigurationService configurationService, ReposeInstanceInfo instanceInfo, DatastoreService datastoreService,
           ServicePorts servicePorts,RoutingService routingService, String configDirectory);
   
   public void startDistributedDatastoreServlet();
   
   public void stopDistributedDatastoreServlet();
   
}
