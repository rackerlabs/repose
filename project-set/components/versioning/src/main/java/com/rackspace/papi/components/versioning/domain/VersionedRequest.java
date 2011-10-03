package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.commons.util.StringUriUtilities;
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
        return StringUriUtilities.formatUri(requestInfo.getUri()).isEmpty();
    }

    public boolean requestBelongsToVersionMapping() {
        final String requestedUri = StringUriUtilities.formatUri(requestInfo.getUri());
        final String versionUri = StringUriUtilities.formatUri(mapping.getId());
        
        return indexOfUriFragment(requestedUri, versionUri) == 0;
    }

    public boolean requestMatchesVersionMapping() {
        final String requestedUri = StringUriUtilities.formatUri(requestInfo.getUri());

        return requestedUri.equals(StringUriUtilities.formatUri(mapping.getId()));
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
        final String formattedUri = StringUriUtilities.formatUri(requestInfo.getUri());
        final String formattedReplacement = StringUriUtilities.formatUri(mapping.getContextPath());

        final int replacementIndex = formattedUri.indexOf(formattedReplacement + "/");

        return replacementIndex < 0;
    }
    
    public static int indexOfUriFragment(String uri, String uriFragment) {
        final int index = uri.indexOf(uriFragment);
        
        if (uri.length() > uriFragment.length() + index) {
            return uri.charAt(index + uriFragment.length()) == '/' ? index : -1;
        }
        
        return index;
    }    
    
    public static int indexOfUriFragment(StringBuilder uri, String uriFragment) {
        final int index = uri.indexOf(uriFragment);
        
        if (uri.length() > uriFragment.length() + index) {
            return uri.charAt(index + uriFragment.length()) == '/' ? index : -1;
        }
        
        return index;
    }
    
    private String updateURI(UniformResourceInfo requestInfo, String original, String replacement) {
        if (requestInfo.getUri().charAt(0) != '/') {
            throw new IllegalArgumentException("Request URI must be a URI with a root reference - i.e. the URI must start with '/'");
        }

        final StringBuilder uriBuilder = new StringBuilder(StringUriUtilities.formatUri(requestInfo.getUri()));
        final String formattedReplacement = StringUriUtilities.formatUri(replacement);
        final String formattedOriginal = StringUriUtilities.formatUri(original);

        final int originalIndex = indexOfUriFragment(uriBuilder, formattedOriginal);
        final int replacementIndex = indexOfUriFragment(uriBuilder, formattedReplacement);

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
