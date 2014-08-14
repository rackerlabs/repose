package com.rackspace.papi.service.context.container;

public interface ContainerConfigurationService {
   
   String getVia();
   
   void setVia(String via);
   
   Long getContentBodyReadLimit();
   
   void setContentBodyReadLimit(Long value);
}
