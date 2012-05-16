package com.rackspace.papi.domain;

import java.util.ArrayList;
import org.springframework.stereotype.Component;

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
