package org.openrepose.filters.versioning.domain;

import org.openrepose.filters.versioning.config.MediaType;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;

public class VersionedMapType {
    
    private MediaType mediaType;
    private ServiceVersionMapping serviceVersionMapping;
    
    public VersionedMapType(ServiceVersionMapping serviceVersionMapping, MediaType mediaType) {
        
        this.mediaType = mediaType;
        this.serviceVersionMapping = serviceVersionMapping;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public ServiceVersionMapping getServiceVersionMapping() {
        return serviceVersionMapping;
    }
    
}
