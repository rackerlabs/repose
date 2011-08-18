package com.rackspace.papi.components.versioning.testhelpers;

import com.rackspace.papi.components.versioning.config.MediaType;
import com.rackspace.papi.components.versioning.config.MediaTypeList;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.config.VersionStatus;
import com.rackspace.papi.model.Host;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/10/11
 * Time: 12:31 PM
 */
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
            mapping.setName("Service Version 1." + i);
            mapping.setPpHostId("service-v1." + i);
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

    public static Map<String, Host> createConfiguredHosts(int numOfHosts) {
        Map<String, Host> hosts = new HashMap<String, Host>();

        for (int i = 0; i < numOfHosts; i++) {
            Host host = new Host();
            host.setId("service-v1." + i);
            host.setHref("http://rackspacecloud.com/v1." + i);

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
            mapping.setName("Service Version 1." + i);
            mapping.setPpHostId("service-v1." + i);
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
