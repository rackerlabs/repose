/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.routing.RoutingService;


public interface DistributedDatastoreLauncherService {
   
   void initialize(ConfigurationService configurationService, ReposeInstanceInfo instanceInfo,
           ServicePorts servicePorts,RoutingService routingService, String configDirectory);
   void startDistributedDatastoreServlet();
   void stopDistributedDatastoreServlet();
}
