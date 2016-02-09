/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.versioning.util;

import org.openrepose.filters.versioning.config.MediaType;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.schema.MediaTypeList;
import org.openrepose.filters.versioning.schema.VersionChoice;
import org.openrepose.filters.versioning.schema.VersionStatus;

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

            for (MediaType configuredMediaType : serviceVersionMapping.getMediaTypes().getMediaType()) {
                org.openrepose.filters.versioning.schema.MediaType responseMediaType = new org.openrepose.filters.versioning.schema.MediaType();

                responseMediaType.setBase(configuredMediaType.getBase());
                responseMediaType.setType(configuredMediaType.getType());

                mediaTypeList.getMediaType().add(responseMediaType);
            }
        }

        return mediaTypeList;
    }
}
