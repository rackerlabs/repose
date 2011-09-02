package com.rackspace.papi.components.versioning.util;


import com.rackspace.papi.components.versioning.config.MediaType;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.schema.MediaTypeList;
import com.rackspace.papi.components.versioning.schema.VersionChoice;
import com.rackspace.papi.components.versioning.schema.VersionStatus;

/**
 * @author fran
 */
public class VersionChoiceFactory {
    private final ServiceVersionMapping serviceVersionMapping;

    public VersionChoiceFactory(ServiceVersionMapping serviceVersionMapping) {
        this.serviceVersionMapping = serviceVersionMapping;
    }

    public VersionChoice create() {
        VersionChoice versionChoice = new VersionChoice();

        versionChoice.setId(serviceVersionMapping.getId());
        versionChoice.setStatus(serviceVersionMapping.getStatus() == null ? null : VersionStatus.fromValue(serviceVersionMapping.getStatus().value()));
        versionChoice.setMediaTypes(convertMediaTypes());

        return versionChoice;
    }

    private MediaTypeList convertMediaTypes() {
        MediaTypeList mediaTypeList = null;

        if (serviceVersionMapping.getMediaTypes() != null) {
            mediaTypeList = new MediaTypeList();

            for (MediaType configuredMediaType : serviceVersionMapping.getMediaTypes().getMediaType()){                
                com.rackspace.papi.components.versioning.schema.MediaType responseMediaType = new com.rackspace.papi.components.versioning.schema.MediaType();

                responseMediaType.setBase(configuredMediaType.getBase());
                responseMediaType.setType(configuredMediaType.getType());

                mediaTypeList.getMediaType().add(responseMediaType);
            }
        }

        return mediaTypeList;
    }
}
