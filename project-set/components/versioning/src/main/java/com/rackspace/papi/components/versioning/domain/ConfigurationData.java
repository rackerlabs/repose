package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.components.versioning.util.http.HttpRequestInfo;
import com.rackspace.papi.components.versioning.util.http.UniformResourceInfo;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.StringUriUtilities;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.components.versioning.config.MediaTypeList;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.schema.VersionChoice;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.util.VersionChoiceFactory;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.commons.util.http.media.servlet.RequestMediaRangeInterrogator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.ietf.atom.schema.Link;
import org.ietf.atom.schema.Relation;

public class ConfigurationData {

    private final Map<String, ServiceVersionMapping> serviceMappings;
    private final Map<String, Host> configuredHosts;
    private final Host localHost;

    public ConfigurationData(Host localHost, Map<String, Host> configuredHosts, Map<String, ServiceVersionMapping> serviceMappings) {
        this.configuredHosts = configuredHosts;
        this.serviceMappings = serviceMappings;
        this.localHost = localHost;
    }

    public Collection<ServiceVersionMapping> getServiceMappings() {
        return serviceMappings.values();
    }

    public Map<String, Host> getConfiguredHosts() {
        return configuredHosts;
    }

    public Host getHostForVersionMapping(ServiceVersionMapping mapping) throws VersionedHostNotFoundException {
        final Host host = configuredHosts.get(mapping.getPpHostId());
        
        if (host == null) {
            throw new VersionedHostNotFoundException("Power Proxy Host: " + mapping.getPpHostId() + " is not specified in the power proxy system model");
        }
        
        return host;
    }

    public VersionedOriginService getOriginServiceForRequest(HttpRequestInfo requestInfo, FilterDirector director) throws VersionedHostNotFoundException {
        // Check URI first to see if it matches configured host href
        VersionedOriginService destination = findOriginServiceByUri(requestInfo);

        // If version info not in URI look in accept header
        if (destination == null) {
            final MediaType range = requestInfo.getPreferedMediaRange();
            final VersionedMapType currentServiceVersion = getServiceVersionForMediaRange(range);
            

            if (currentServiceVersion != null) {
                final Host host = getHostForVersionMapping(currentServiceVersion.getServiceVersionMapping());
                director.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.toString(), currentServiceVersion.getMediaType().getBase());
                destination = new VersionedOriginService(currentServiceVersion.getServiceVersionMapping(),host);
            }
        }

        return destination;
    }

    public VersionedOriginService findOriginServiceByUri(HttpRequestInfo requestResourceInfo) throws VersionedHostNotFoundException {
        for (Map.Entry<String, ServiceVersionMapping> entry : serviceMappings.entrySet()) {
            final VersionedRequest versionedRequest = new VersionedRequest(requestResourceInfo, entry.getValue());

            if (versionedRequest.requestBelongsToVersionMapping()) {
                return new VersionedOriginService(entry.getValue(), getHostForVersionMapping(entry.getValue()));
            }
        }

        return null;
    }

    public VersionChoiceList versionChoicesAsList(HttpRequestInfo requestResourceInfo) {
        final VersionChoiceList versionChoices = new VersionChoiceList();

        for (ServiceVersionMapping mapping : getServiceMappings()) {
            final VersionedRequest versionedRequest = new VersionedRequest(requestResourceInfo, mapping);
            final VersionChoice choice = new VersionChoiceFactory(mapping).create();
            final Link selfReference = new Link();

            selfReference.setRel(Relation.SELF);
            selfReference.setHref(versionedRequest.asExternalURL());

            choice.getLink().add(selfReference);
            versionChoices.getVersion().add(choice);
        }

        return versionChoices;
    }

    public VersionedMapType getServiceVersionForMediaRange(MediaType preferedMediaRange) {
        com.rackspace.papi.components.versioning.config.MediaType mediaType;
        for (Map.Entry<String, ServiceVersionMapping> serviceMapping : serviceMappings.entrySet()) {
            mediaType = getMatchingMediaType((ServiceVersionMapping)serviceMapping.getValue(), preferedMediaRange);
            if(mediaType != null){
                return new VersionedMapType((ServiceVersionMapping)serviceMapping.getValue(), mediaType);
            }
           
        }

        return null;
    }
    
    public com.rackspace.papi.components.versioning.config.MediaType getMatchingMediaType(ServiceVersionMapping serviceVersionMapping, MediaType preferedMediaType) {
        final MediaTypeList configuredMediaTypes = serviceVersionMapping.getMediaTypes();
        for (com.rackspace.papi.components.versioning.config.MediaType configuredMediaType : configuredMediaTypes.getMediaType()) {
            List<MediaType> mediaTypes = RequestMediaRangeInterrogator.interrogate("", configuredMediaType.getType());
            if(mediaTypes.contains(preferedMediaType)){
                return configuredMediaType;
            }
             
        }

        return null;
    }

    public boolean isRequestForVersions(UniformResourceInfo uniformResourceInfo) {
        return StringUriUtilities.formatUri(uniformResourceInfo.getUri()).equals("/");
    }

    public Host getLocalHost() {
      return localHost;
    }
}
