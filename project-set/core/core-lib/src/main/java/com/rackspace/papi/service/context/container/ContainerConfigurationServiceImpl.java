package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.Port;
import java.util.ArrayList;
import java.util.List;

public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {
   private final List<Port> ports = new ArrayList<Port>();

   public ContainerConfigurationServiceImpl() {
   }
   
   public ContainerConfigurationServiceImpl(List<Port> ports) {
      this.ports.clear();
      this.ports.addAll(ports);
   }
   
   @Override
   public List<Port> getPorts() {
      return ports;
   }
}
