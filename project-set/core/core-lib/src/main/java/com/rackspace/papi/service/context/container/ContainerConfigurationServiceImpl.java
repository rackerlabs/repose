package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.ServicePorts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component("containerConfigurationService")
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {

    private final ServicePorts ports = new ServicePorts();
    private String viaValue;
    private Long contentBodyReadLimit;

    public ContainerConfigurationServiceImpl() {
    }

    @Autowired
    public ContainerConfigurationServiceImpl(@Qualifier("servicePorts") ServicePorts ports) {
        this.ports.addAll(ports);
    }

    public ContainerConfigurationServiceImpl(String via, Long contentBodyReadLimit, ServicePorts ports) {

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
    public Long getContentBodyReadLimit() {
        if (contentBodyReadLimit == null) {
            return Long.valueOf(0);
        } else {
            return contentBodyReadLimit;
        }
    }

    @Override
    public void setContentBodyReadLimit(Long value) {
        this.contentBodyReadLimit = value;
    }

   @Override
   public ServicePorts getServicePorts() {
      return ports;
   }
}
