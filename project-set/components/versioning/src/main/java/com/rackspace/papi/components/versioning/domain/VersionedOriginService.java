package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.model.Host;

public class VersionedOriginService {

    private final ServiceVersionMapping mapping;
    private final Host originServiceHost;

    public VersionedOriginService(ServiceVersionMapping mapping, Host originServiceHost) {
        this.mapping = mapping;
        this.originServiceHost = originServiceHost;
    }

    public ServiceVersionMapping getMapping() {
        return mapping;
    }

    public Host getOriginServiceHost() {
        return originServiceHost;
    }
}
