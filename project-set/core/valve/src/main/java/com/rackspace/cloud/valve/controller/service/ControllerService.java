
package com.rackspace.cloud.valve.controller.service;

import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.model.Node;
import java.util.Map;
import java.util.Set;


public interface ControllerService {
   
   
   Set<String> getManagedInstances();
   
   void updateManagedInstances(Map<String, ExtractorResult<Node>> nodesToStart, Set<String> nodesToStop);
   
   Boolean reposeInstancesInitialized();
   
   void setConfigDirectory(String directory);
   
   void setConnectionFramework(String framework);
   
   String getConfigDirectory();
   
   String getConnectionFramework();
   
   void setIsInsecure(boolean isInsecure);
   
   Boolean isInsecure();
}
