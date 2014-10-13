package org.openrepose.filters.versioning.domain;

import org.openrepose.core.systemmodel.Destination;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;

public class VersionedOriginService {

    private final ServiceVersionMapping mapping;
    private final Destination originServiceHost;

    public VersionedOriginService(ServiceVersionMapping mapping, Destination originServiceHost) {
        this.mapping = mapping;
        this.originServiceHost = originServiceHost;
    }

    public ServiceVersionMapping getMapping() {
        return mapping;
    }

    public Destination getOriginServiceHost() {
        return originServiceHost;
    }
}
