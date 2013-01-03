package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;

import java.util.List;

public interface ContainerConfigurationService {
   
   String getVia();
   
   void setVia(String via);
   
   int getContentBodyReadLimit();
   
   void setContentBodyReadLimit(int value);
   
   ServicePorts getServicePorts();
   
}
