

package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.components.versioning.config.MediaType;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;


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
