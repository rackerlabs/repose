
package com.rackspace.cloud.valve.controller.service;

import org.openrepose.commons.utils.regex.ExtractorResult;
import com.rackspace.papi.model.Node;
import java.util.Map;
import java.util.Set;


public interface ControllerService {
   
   
   Set<String> getManagedInstances();
   
   void updateManagedInstances(Map<String, ExtractorResult<Node>> nodesToStart, Set<String> nodesToStop);
   
   Boolean reposeInstancesInitialized();
   
   void setConfigDirectory(String directory);
   
   String getConfigDirectory();
   
   void setIsInsecure(boolean isInsecure);
   
   Boolean isInsecure();
}
