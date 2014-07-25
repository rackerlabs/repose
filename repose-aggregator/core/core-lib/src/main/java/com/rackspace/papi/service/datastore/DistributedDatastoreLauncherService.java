/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import org.openrepose.core.service.config.ConfigurationService;


public interface DistributedDatastoreLauncherService {

    void initialize(ConfigurationService configurationService, ReposeInstanceInfo instanceInfo, String configDirectory);

    void startDistributedDatastoreServlet();

    void stopDistributedDatastoreServlet();
}
