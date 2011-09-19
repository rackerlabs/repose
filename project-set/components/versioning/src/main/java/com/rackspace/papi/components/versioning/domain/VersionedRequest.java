package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.UniformResourceInfo;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;

public class VersionedRequest {

    private final HttpRequestInfo requestInfo;
    private final ServiceVersionMapping mapping;
    private final String serviceRootUrl;

    public VersionedRequest(HttpRequestInfo requestInfo, ServiceVersionMapping mapping, String serviceRootUrl) {
        this.requestInfo = requestInfo;
        this.mapping = mapping;
        this.serviceRootUrl = serviceRootUrl.endsWith("/") ? serviceRootUrl.substring(0, serviceRootUrl.length() - 1) : serviceRootUrl;
    }

    public ServiceVersionMapping getMapping() {
        return mapping;
    }

    public HttpRequestInfo getRequestInfo() {
        return requestInfo;
    }

    public String getServiceRootUrl() {
        return serviceRootUrl;
    }
    
    public boolean isRequestForRoot() {
        return formatUri(requestInfo.getUri()).isEmpty();
    }
    
    public boolean requestBelongsToVersionMapping() {
        final String requestedUri = formatUri(requestInfo.getUri());

        return requestedUri.startsWith(formatUri(mapping.getId()));
    }

    public boolean requestMatchesVersionMapping() {
        final String requestedUri = formatUri(requestInfo.getUri());

        return requestedUri.equals(formatUri(mapping.getId()));
    }

    public String asExternalURL() {
        return serviceRootUrl + asExternalURI();
    }

    public String asExternalURI() {
        return updateURI(requestInfo, formatUri(mapping.getContextPath()), formatUri(mapping.getId()));
    }

    public String asInternalURL() {
        return serviceRootUrl + asInternalURI();
    }

    public String asInternalURI() {
        return updateURI(requestInfo, formatUri(mapping.getId()), formatUri(mapping.getContextPath()));
    }

    private String updateURI(UniformResourceInfo requestInfo, String target, String replacement) {
        if (requestInfo.getUri().charAt(0) != '/') {
            throw new IllegalArgumentException("Request URI must be a URI with a root reference - i.e. the URI must start with '/'");
        }

        final StringBuilder uriBuilder = new StringBuilder(formatUri(requestInfo.getUri()));
        final String formattedReplacement = formatUri(replacement);
        final String formattedTarget = formatUri(target);

        final int mappingUriIndex = uriBuilder.indexOf(formattedTarget);

        if (mappingUriIndex < 0) {
            uriBuilder.insert(0, formattedReplacement);
        } else {
            uriBuilder.delete(mappingUriIndex, mappingUriIndex + formattedTarget.length());
            uriBuilder.insert(mappingUriIndex, formattedReplacement);
        }

        return uriBuilder.toString();
    }

    /**
     * Formats a URI by adding a forward slash and removing the last forward
     * slash from the URI.
     * 
     * e.g. some/random/uri/    -> /some/random/uri
     * e.g. some/random/uri     -> /some/random/uri
     * e.g. /some/random/uri/   -> /some/random/uri
     * 
     * @param uri
     * @return 
     */
    public static String formatUri(String uri) {
        if (StringUtilities.isBlank(uri)) {
          return "";
        }
        
        final StringBuilder externalName = new StringBuilder(uri);

        if (externalName.charAt(0) != '/') {
            externalName.insert(0, "/");
        }

        if (externalName.charAt(externalName.length() - 1) == '/') {
            externalName.deleteCharAt(externalName.length() - 1);
        }

        return externalName.toString();
    }
}
