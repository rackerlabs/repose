package com.rackspace.papi.service.context.container;

public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {
   private final int port;

   public ContainerConfigurationServiceImpl() {
      port = -1;
   }
   
   public ContainerConfigurationServiceImpl(int port) {
      this.port = port;
   }
   
   @Override
   public int getPort() {
      return port;
   }
}
