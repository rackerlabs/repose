package com.rackspace.papi.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component("servicePorts")
public class ServicePorts extends ArrayList<Port> {
    
    public ServicePorts() {
        super();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Port port: this) {
            builder.append(port.getProtocol()).append(": ").append(port.getPort());
        }
        return builder.toString();
    }
}
