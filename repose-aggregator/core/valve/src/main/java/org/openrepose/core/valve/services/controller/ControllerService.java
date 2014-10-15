
package org.openrepose.core.valve.services.controller;

import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.core.systemmodel.Node;
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
