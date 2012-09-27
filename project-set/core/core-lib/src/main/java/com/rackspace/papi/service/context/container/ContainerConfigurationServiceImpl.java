package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.Port;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("containerConfigurationService")
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {
   private final List<Port> ports = new ArrayList<Port>();
   private String viaValue;
   
   public ContainerConfigurationServiceImpl() {
   }
   
   public ContainerConfigurationServiceImpl(List<Port> ports, String via) {
      this.ports.addAll(ports);
      this.viaValue = via;
   }
   
   @Override
   public List<Port> getPorts() {
      return ports;
   }
   
   @Override
   public String getVia(){
      return viaValue;
   }
   
   @Override
   public void setVia(String via){
      this.viaValue = via;
   }
}
