package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.ServicePorts;
import org.springframework.stereotype.Component;


@Component("containerConfigurationService")
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {

    private final ServicePorts ports = new ServicePorts();
    private String viaValue;
    private int contentBodyReadLimit;

    public ContainerConfigurationServiceImpl() {
    }

    public ContainerConfigurationServiceImpl(String via, int contentBodyReadLimit, ServicePorts ports) {
        this.ports.addAll(ports);
        this.viaValue = via;
        this.contentBodyReadLimit = contentBodyReadLimit;
    }


    @Override
    public String getVia() {
        return viaValue;
    }

    @Override
    public void setVia(String via) {
        this.viaValue = via;
    }

    @Override
    public int getContentBodyReadLimit() {
        if (contentBodyReadLimit <= 1) {
            return 0;
        } else {
            return contentBodyReadLimit;
        }
    }

    @Override
    public void setContentBodyReadLimit(int value) {
        this.contentBodyReadLimit = value;
    }

   @Override
   public ServicePorts getServicePorts() {
      return ports;
   }
}
