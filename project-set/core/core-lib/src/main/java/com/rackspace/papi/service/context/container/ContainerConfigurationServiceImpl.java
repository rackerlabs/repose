package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.Port;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("containerConfigurationService")
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {
   private final List<Port> ports = new ArrayList<Port>();

   public ContainerConfigurationServiceImpl() {
   }
   
   public ContainerConfigurationServiceImpl(List<Port> ports) {
      this.ports.addAll(ports);
   }
   
   @Override
   public List<Port> getPorts() {
      return ports;
   }
}
