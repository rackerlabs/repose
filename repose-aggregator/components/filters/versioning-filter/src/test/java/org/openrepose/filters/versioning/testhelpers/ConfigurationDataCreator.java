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
package org.openrepose.filters.versioning.testhelpers;

import org.openrepose.core.systemmodel.config.Destination;
import org.openrepose.filters.versioning.config.MediaType;
import org.openrepose.filters.versioning.config.MediaTypeList;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.config.VersionStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class ConfigurationDataCreator {

    public static Set<String> createVersionIds(int numOfMappings) {
        Map<String, ServiceVersionMapping> mappings = createConfiguredMappings(numOfMappings);

        return mappings.keySet();
    }

    public static Map<String, ServiceVersionMapping> createConfiguredMappings(int numOfMappings) {
        Map<String, ServiceVersionMapping> mappings = new HashMap<String, ServiceVersionMapping>();

        String id;
        ServiceVersionMapping mapping;
        for (int i = 0; i < numOfMappings; i++) {
            id = "v1." + i;

            mapping = new ServiceVersionMapping();
            mapping.setId(id);
            mapping.setPpDestId("service-v1." + i);
            mapping.setStatus(VersionStatus.CURRENT);

            MediaType mediaType;
            MediaTypeList mediaTypes = new MediaTypeList();

            mediaType = new MediaType();
            mediaType.setBase("application/xml");
            mediaType.setType("application/vnd.vendor.service-v1." + i + "+xml");
            mediaTypes.getMediaType().add(mediaType);

            mediaType = new MediaType();
            mediaType.setBase("application/json");
            mediaType.setType("application/vnd.vendor.service-v1." + i + "+json");
            mediaTypes.getMediaType().add(mediaType);

            mapping.setMediaTypes(mediaTypes);

            mappings.put(id, mapping);
        }

        return mappings;
    }

    public static Map<String, Destination> createConfiguredHosts(int numOfHosts) {
        Map<String, Destination> hosts = new HashMap<String, Destination>();

        for (int i = 0; i < numOfHosts; i++) {
            Destination host = new Destination();
            host.setId("service-v1." + i);

            hosts.put("service-v1." + i, host);
        }

        return hosts;
    }

    public static Map<String, ServiceVersionMapping> createConfiguredMappingsWithAcceptParameters(int numOfMappings) {
        Map<String, ServiceVersionMapping> mappings = new HashMap<String, ServiceVersionMapping>();

        String id;
        ServiceVersionMapping mapping;
        for (int i = 0; i < numOfMappings; i++) {
            id = "v1." + i;

            mapping = new ServiceVersionMapping();
            mapping.setId(id);
            mapping.setPpDestId("service-v1." + i);
            mapping.setStatus(VersionStatus.CURRENT);

            MediaType mediaType;
            MediaTypeList mediaTypes = new MediaTypeList();

            mediaType = new MediaType();
            mediaType.setBase("application/xml");
            mediaType.setType("application/vnd.vendor.service; x=v1." + i + "; y=xml");
            mediaTypes.getMediaType().add(mediaType);

            mediaType = new MediaType();
            mediaType.setBase("application/json");
            mediaType.setType("application/vnd.vendor.service; x=v1." + i + "; y=json");
            mediaTypes.getMediaType().add(mediaType);

            mapping.setMediaTypes(mediaTypes);

            mappings.put(id, mapping);
        }

        return mappings;
    }

}
