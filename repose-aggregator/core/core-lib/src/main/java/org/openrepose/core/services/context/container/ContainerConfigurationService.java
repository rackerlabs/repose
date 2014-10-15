package org.openrepose.core.services.context.container;

import org.openrepose.core.domain.ServicePorts;


public interface ContainerConfigurationService {
   
   String getVia();
   
   void setVia(String via);
   
   Long getContentBodyReadLimit();
   
   ServicePorts getServicePorts();
   
   void setContentBodyReadLimit(Long value);
}
