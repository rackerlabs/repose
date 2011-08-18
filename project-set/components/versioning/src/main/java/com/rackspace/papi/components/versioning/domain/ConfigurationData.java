package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.UniformResourceInfo;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;

import com.rackspace.papi.components.versioning.config.MediaType;
import com.rackspace.papi.components.versioning.config.MediaTypeList;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.schema.VersionChoice;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.util.VersionChoiceFactory;
import com.rackspace.papi.model.Host;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ietf.atom.schema.Link;
import org.ietf.atom.schema.Relation;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/7/11
 * Time: 10:39 AM
 */
public class ConfigurationData {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ConfigurationData.class);

    private final Map<String, ServiceVersionMapping> serviceMappings;
    private final Map<String, Host> configuredHosts;
    private final String serviceRootHref;

    public ConfigurationData(String serviceRootHref, Map<String, Host> configuredHosts,
                             Map<String, ServiceVersionMapping> serviceMappings) {
        this.serviceRootHref = serviceRootHref.endsWith("/") ? serviceRootHref : serviceRootHref + "/";
        this.configuredHosts = configuredHosts;
        this.serviceMappings = serviceMappings;
    }

    public String getServiceVersionBasedOnAcceptHeader(String acceptHeader) {
        String serviceVersion = null;

        if (!StringUtilities.isBlank(acceptHeader)) {
            List<MediaRange> mediaRanges = MediaRangeParser.getMediaRangesFromAcceptHeader(acceptHeader);

            for (Map.Entry serviceMapping : serviceMappings.entrySet()) {
                if (mediaTypeMatchesVersionConfigServiceMapping((ServiceVersionMapping) serviceMapping.getValue(), mediaRanges)) {
                    serviceVersion = (String) serviceMapping.getKey();
                    break;
                }
            }
        }
        
        return serviceVersion;
    }

    private boolean mediaTypeMatchesVersionConfigServiceMapping(ServiceVersionMapping serviceVersionMapping, List<MediaRange> mediaRanges) {
        for (MediaRange requestMediaRange : mediaRanges) {
            MediaTypeList configuredMediaTypes = serviceVersionMapping.getMediaTypes();
            
            for (MediaType configuredMediaType : configuredMediaTypes.getMediaType()) {
                MediaRange configuredMediaRange = MediaRangeParser.parseMediaRange(configuredMediaType.getType());

                if (requestMediaRange.equals(configuredMediaRange)) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getServiceRootHref() {
        return serviceRootHref;
    }

    public Collection<ServiceVersionMapping> getServiceMappings() {
        return serviceMappings.values();
    }

    public Host mapOriginHostFromVersionId(String currentServiceVersion) {
        final ServiceVersionMapping mapping = serviceMappings.get(currentServiceVersion);
        Host destination = null;

        if (mapping != null) {
            destination = configuredHosts.get(mapping.getPpHostId());

            if (destination == null) {
                LOG.warn("Configured versioned service mapping: "
                        + mapping.getName()
                        + " does not map to a valid host in the system model. Expected host id was: "
                        + mapping.getPpHostId());
            }
        }

        return destination;
    }

    public Host getOriginToRouteTo(HttpRequestInfo requestInfo) {
        // Check URI first to see if it matches configured host href
        Host destination = findHost(requestInfo);

        // If version info not in URI look in accept header
        if (destination == null) {
            final String currentServiceVersion = getServiceVersionBasedOnAcceptHeader(requestInfo.getAcceptHeader());

            if (currentServiceVersion != null) {
                destination = mapOriginHostFromVersionId(currentServiceVersion);
            }
        }

        return destination;
    }

    public Host findHost(UniformResourceInfo requestResourceInfo) {
        Host destination = null;

        for (Map.Entry<String, ServiceVersionMapping> entry : serviceMappings.entrySet()) {
            if (matchesHostUrl(requestResourceInfo, serviceRootHref + entry.getValue().getId())) {
                destination = configuredHosts.get(entry.getValue().getPpHostId());
                break;
            }
        }

        return destination;
    }

    public VersionChoiceList versionChoicesAsList(UniformResourceInfo requestResourceInfo) {
        final VersionChoiceList versionChoices = new VersionChoiceList();

        for (ServiceVersionMapping mapping : getServiceMappings()) {
            final Link selfReference = new Link();
            selfReference.setRel(Relation.SELF);

            final VersionChoice choice = new VersionChoiceFactory(mapping).create();

            selfReference.setHref(createSelfReference(getServiceRootHref(), requestResourceInfo, choice));

            choice.getLink().add(selfReference);

            versionChoices.getVersion().add(choice);
        }

        return versionChoices;
    }

    public boolean isRequestForVersionChoices(UniformResourceInfo uniformResourceInfo) {
        return uniformResourceInfo.getUrl().startsWith(getServiceRootHref()) ||
                uniformResourceInfo.getUrl().equals(getServiceRootHref());
    }

    public static boolean matchesHostUrl(UniformResourceInfo requestResourceInfo, String hostHref) {
        if(requestResourceInfo == null) {
            throw new IllegalArgumentException("requestResourceInfo can not be null");
        }

        return !StringUtilities.isBlank(hostHref) &&
                hostHref.matches("^http[s]?://.+") &&
                requestResourceInfo.getUrl().startsWith(hostHref);
    }

    public static String createSelfReference(String serviceRootHref, UniformResourceInfo requestResourceInfo, VersionChoice choice) {
        return serviceRootHref + choice.getId() + requestResourceInfo.getUri();
    }

    public Set<String> getVersionIds() {
        return serviceMappings.keySet();
    }
}
