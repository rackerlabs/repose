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
        return StringUtilities.formatUri(requestInfo.getUri()).isEmpty();
    }

    public boolean requestBelongsToVersionMapping() {
        final String requestedUri = StringUtilities.formatUri(requestInfo.getUri());

        return requestedUri.startsWith(StringUtilities.formatUri(mapping.getId()));
    }

    public boolean requestMatchesVersionMapping() {
        final String requestedUri = StringUtilities.formatUri(requestInfo.getUri());

        return requestedUri.equals(StringUtilities.formatUri(mapping.getId()));
    }

    public String asExternalURL() {
        return serviceRootUrl + asExternalURI();
    }

    public String asExternalURI() {
        return updateURI(requestInfo, mapping.getContextPath(), mapping.getId());
    }

    public String asInternalURL() {
        return serviceRootUrl + asInternalURI();
    }

    public String asInternalURI() {
        return updateURI(requestInfo, mapping.getId(), mapping.getContextPath());
    }

    public boolean uriRequiresRewrite() {
        final String formattedUri = StringUtilities.formatUri(requestInfo.getUri());
        final String formattedReplacement = StringUtilities.formatUri(mapping.getContextPath());

        final int replacementIndex = formattedUri.indexOf(formattedReplacement + "/");

        return replacementIndex < 0;
    }

    private String updateURI(UniformResourceInfo requestInfo, String original, String replacement) {
        if (requestInfo.getUri().charAt(0) != '/') {
            throw new IllegalArgumentException("Request URI must be a URI with a root reference - i.e. the URI must start with '/'");
        }

        final StringBuilder uriBuilder = new StringBuilder(StringUtilities.formatUri(requestInfo.getUri()));
        final String formattedReplacement = StringUtilities.formatUri(replacement);
        final String formattedOriginal = StringUtilities.formatUri(original);

        final int originalIndex = uriBuilder.indexOf(formattedOriginal + "/");
        final int replacementIndex = uriBuilder.indexOf(formattedReplacement + "/");

        if (replacementIndex < 0) {
            if (originalIndex < 0) {
                uriBuilder.insert(0, formattedReplacement);
            } else {
                uriBuilder.delete(originalIndex, originalIndex + formattedOriginal.length());
                uriBuilder.insert(originalIndex, formattedReplacement);
            }
        }

        return uriBuilder.toString();
    }

}
