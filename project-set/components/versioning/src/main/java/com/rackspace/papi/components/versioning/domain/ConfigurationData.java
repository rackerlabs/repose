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

import java.util.Collection;
import java.util.Map;
import org.ietf.atom.schema.Link;
import org.ietf.atom.schema.Relation;

public class ConfigurationData {
    
    private final Map<String, ServiceVersionMapping> serviceMappings;
    private final Map<String, Host> configuredHosts;
    private final String serviceRootHref;
    
    public ConfigurationData(String serviceRootHref, Map<String, Host> configuredHosts, Map<String, ServiceVersionMapping> serviceMappings) {
        this.serviceRootHref = removeLastForwardSlash(serviceRootHref);
        this.configuredHosts = configuredHosts;
        this.serviceMappings = serviceMappings;
    }
    
    public static String removeLastForwardSlash(String st) {
        return st.endsWith("/") && st.length() > 1 ? st.substring(0, st.length() - 1) : st;
    }
    
    public String getServiceRootHref() {
        return serviceRootHref;
    }
    
    public Collection<ServiceVersionMapping> getServiceMappings() {
        return serviceMappings.values();
    }
    
    public Map<String, Host> getConfiguredHosts() {
        return configuredHosts;
    }
    
    public String buildInternalVersionedUri(ServiceVersionMapping mapping, UniformResourceInfo requestInfo) {
        final String externalVersionRoot = "/" + mapping.getName();
        final StringBuilder uriBuilder = new StringBuilder("/").append(mapping.getId());
        final int substringOffset = requestInfo.getUri().startsWith(externalVersionRoot) ? externalVersionRoot.length() : 0;
        
        uriBuilder.append(requestInfo.getUri().substring(substringOffset));
        return uriBuilder.toString();
    }
    
    public String buildInternalVersionedUrl(ServiceVersionMapping mapping, UniformResourceInfo requestInfo) {
        return serviceRootHref + buildInternalVersionedUri(mapping, requestInfo);
    }
    
    public String buildExternalVersionedUri(ServiceVersionMapping mapping, UniformResourceInfo requestInfo) {
        final StringBuilder uriBuilder = new StringBuilder("/").append(mapping.getName());
        final int substringOffset = requestInfo.getUri().startsWith(uriBuilder.toString()) ? uriBuilder.length() : 0;
        
        uriBuilder.append(requestInfo.getUri().substring(substringOffset));
        return uriBuilder.toString();
    }
    
    public String buildExternalVersionedUrl(ServiceVersionMapping mapping, UniformResourceInfo requestInfo) {
        return serviceRootHref + buildExternalVersionedUri(mapping, requestInfo);
    }
    
    public VersionedOriginService getOriginServiceForRequest(HttpRequestInfo requestInfo) {
        // Check URI first to see if it matches configured host href
        VersionedOriginService destination = findOriginServiceByUri(requestInfo);

        // If version info not in URI look in accept header
        if (destination == null) {
            final ServiceVersionMapping currentServiceVersion = getServiceVersionForMediaRange(requestInfo.getPreferedMediaRange());
            
            if (currentServiceVersion != null) {
                destination = new VersionedOriginService(currentServiceVersion, getHostForVersionMapping(currentServiceVersion));
            }
        }
        
        return destination;
    }
    
    public VersionedOriginService findOriginServiceByUri(UniformResourceInfo requestResourceInfo) {
        for (Map.Entry<String, ServiceVersionMapping> entry : serviceMappings.entrySet()) {
            if (requestUriMatchesVersionedUri(requestResourceInfo, entry.getValue().getName())) {
                return new VersionedOriginService(entry.getValue(), getHostForVersionMapping(entry.getValue()));
            }
        }
        
        return null;
    }
    
    public Host getHostForVersionMapping(ServiceVersionMapping mapping) {
        return configuredHosts.get(mapping.getPpHostId());
    }
    
    public VersionChoiceList versionChoicesAsList(UniformResourceInfo requestResourceInfo) {
        final VersionChoiceList versionChoices = new VersionChoiceList();
        
        for (ServiceVersionMapping mapping : getServiceMappings()) {
            final VersionChoice choice = new VersionChoiceFactory(mapping).create();
            final Link selfReference = new Link();
            
            selfReference.setRel(Relation.SELF);
            selfReference.setHref(buildExternalVersionedUrl(mapping, requestResourceInfo));
            
            choice.getLink().add(selfReference);
            versionChoices.getVersion().add(choice);
        }
        
        return versionChoices;
    }
    
    public ServiceVersionMapping getServiceVersionForMediaRange(MediaRange preferedMediaRange) {
        for (Map.Entry<String, ServiceVersionMapping> serviceMapping : serviceMappings.entrySet()) {
            if (mediaTypeMatchesVersionConfigServiceMapping((ServiceVersionMapping) serviceMapping.getValue(), preferedMediaRange)) {
                return serviceMapping.getValue();
            }
        }
        
        return null;
    }
    
    private boolean mediaTypeMatchesVersionConfigServiceMapping(ServiceVersionMapping serviceVersionMapping, MediaRange preferedMediaRange) {
        final MediaTypeList configuredMediaTypes = serviceVersionMapping.getMediaTypes();
        
        for (MediaType configuredMediaType : configuredMediaTypes.getMediaType()) {
            final MediaRange configuredMediaRange = MediaRangeParser.parseMediaRange(configuredMediaType.getType());
            
            if (preferedMediaRange.equals(configuredMediaRange)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isDescribeVersionsRequest(UniformResourceInfo uniformResourceInfo) {
        return removeLastForwardSlash(uniformResourceInfo.getUrl()).equals(getServiceRootHref());
    }
    
    public static boolean requestUriMatchesVersionedUri(UniformResourceInfo requestResourceInfo, String versionName) {
        if (requestResourceInfo == null) {
            throw new IllegalArgumentException("requestResourceInfo can not be null");
        }
        
        return requestResourceInfo.getUri().startsWith("/" + versionName);
    }
}
