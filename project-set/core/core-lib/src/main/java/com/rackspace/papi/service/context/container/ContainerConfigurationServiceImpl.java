package com.rackspace.papi.service.context.container;

import com.rackspace.papi.domain.Port;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("containerConfigurationService")
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {

    private final List<Port> ports = new ArrayList<Port>();
    private String viaValue;
    private Long contentBodyReadLimit;

    public ContainerConfigurationServiceImpl() {
    }

    public ContainerConfigurationServiceImpl(List<Port> ports, String via, Long contentBodyReadLimit) {
        this.ports.addAll(ports);
        this.viaValue = via;
        this.contentBodyReadLimit = contentBodyReadLimit;
    }

    @Override
    public List<Port> getPorts() {
        return ports;
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
            return new Long(0);
        } else {
            return contentBodyReadLimit;
        }
    }

    @Override
    public void setContentBodyReadLimit(Long value) {
        this.contentBodyReadLimit = value;
    }
}
