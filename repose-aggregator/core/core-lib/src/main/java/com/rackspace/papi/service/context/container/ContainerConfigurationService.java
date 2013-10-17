package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.ServicePorts;


public interface ContainerConfigurationService {
   
   String getVia();
   
   void setVia(String via);
   
   Long getContentBodyReadLimit();
   
   ServicePorts getServicePorts();
   
   void setContentBodyReadLimit(Long value);
}
