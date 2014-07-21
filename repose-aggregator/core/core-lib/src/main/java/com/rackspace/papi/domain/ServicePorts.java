package com.rackspace.papi.domain;

import javax.inject.Named;

import java.util.ArrayList;
import java.util.List;

/**
 * Can't quite delete it yet, because it's in all the things, will be easier later
 * TODO: use ReposeInstanceInfo instead, it provides a list of ports (not the "ServicePorts" class)
 * This is a bad abstraction.
 */
@Deprecated()
@Named("servicePorts")
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
