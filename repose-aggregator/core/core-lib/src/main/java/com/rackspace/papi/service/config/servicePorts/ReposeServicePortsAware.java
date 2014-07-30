package com.rackspace.papi.service.config.servicePorts;

import com.rackspace.papi.domain.Port;

import java.util.List;

/**
 * This interface provides the callback to tell things about when their service ports have been updated.
 */
public interface ReposeServicePortsAware {
    public void updatedServicePorts(List<Port> ports);
}
