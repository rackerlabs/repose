package com.rackspace.papi.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("servicePorts")
public class ServicePorts extends ArrayList<Port> {
    
    public ServicePorts() {
        super();
    }

    public List<Integer> getPorts() {
        List<Integer> ports = new ArrayList<Integer>();

        for (Port port: this) {
            ports.add(port.getPort());
        }

        return ports;
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
