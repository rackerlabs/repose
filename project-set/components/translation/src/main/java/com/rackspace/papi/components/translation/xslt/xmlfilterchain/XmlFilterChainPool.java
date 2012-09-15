package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.components.translation.config.HttpMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class XmlFilterChainPool {

    private final String contentType;
    private final boolean acceptAllContentTypes;
    private final String accept;
    private final boolean acceptAll;
    private final Pool<XsltFilterChain> pool;
    private final String resultContentType;
    private final Pattern statusRegex;
    private boolean allMethods;
    private final List<HttpMethod> httpMethods;
    private final List<Parameter> params;

    /**
     * Response XSLT Handler Chain Pools
     * 
     * @param contentType
     * @param accept
     * @param statusRegex
     * @param resultContentType
     * @param params
     * @param pool 
     */
    public XmlFilterChainPool(String contentType, String accept, String statusRegex, String resultContentType, List<Parameter> params, Pool<XsltFilterChain> pool) {
        this.contentType = contentType;
        this.acceptAllContentTypes = StringUtilities.nullSafeEqualsIgnoreCase(this.contentType, MimeType.WILDCARD.getMimeType());
        this.accept = accept;
        this.acceptAll = StringUtilities.nullSafeEqualsIgnoreCase(this.accept, MimeType.WILDCARD.getMimeType());
        this.resultContentType = resultContentType;
        this.pool = pool;
        this.httpMethods = new ArrayList<HttpMethod>();
        this.statusRegex = StringUtilities.isNotBlank(statusRegex) ? Pattern.compile(statusRegex) : null;
        this.allMethods = true;
        this.params = params;
    }
    
    /**
     * Request XSLT Handler Chain Pools
     * 
     * @param contentType
     * @param accept
     * @param httpMethods
     * @param resultContentType
     * @param params
     * @param pool 
     */
    public XmlFilterChainPool(String contentType, String accept, List<HttpMethod> httpMethods, String resultContentType, List<Parameter> params, Pool<XsltFilterChain> pool) {
        this.contentType = contentType;
        this.acceptAllContentTypes = StringUtilities.nullSafeEqualsIgnoreCase(this.contentType, MimeType.WILDCARD.getMimeType());
        this.accept = accept;
        this.acceptAll = StringUtilities.nullSafeEqualsIgnoreCase(this.accept, MimeType.WILDCARD.getMimeType());
        this.resultContentType = resultContentType;
        this.pool = pool;
        this.httpMethods = httpMethods;
        this.statusRegex = null;
        for (HttpMethod method : httpMethods) {
            this.allMethods |= "ALL".equalsIgnoreCase(method.name());
        }
        this.params = params;
    }
    
    private boolean matchesMethod(String requestMethod) {
        boolean result = false;
        for (HttpMethod method : httpMethods) {
            result |= method.name().equalsIgnoreCase(requestMethod);
        }
        
        return result;
    }

    public boolean accepts(String method, MediaType contentType, MediaType accept, String statusCode) {
        boolean matchesAccept = acceptAll || StringUtilities.nullSafeEqualsIgnoreCase(this.accept, accept.getValue());
        boolean matchesContentType = acceptAllContentTypes || StringUtilities.nullSafeEqualsIgnoreCase(this.contentType, contentType.getValue());
        boolean matchesStatusCode = statusRegex != null && StringUtilities.isNotBlank(statusCode) ? statusRegex.matcher(statusCode).matches() : true;
        boolean matchesMethod = StringUtilities.isNotBlank(method) ? allMethods || matchesMethod(method) : true;

        return matchesAccept && matchesContentType && matchesStatusCode && matchesMethod;
    }

    public String getContentType() {
        return contentType;
    }

    public String getAccept() {
        return accept;
    }

    public Pool<XsltFilterChain> getPool() {
        return pool;
    }

    public String getResultContentType() {
        return resultContentType;
    }

    public Pattern getStatusRegex() {
        return statusRegex;
    }

    public List<Parameter> getParams() {
        return params;
    }
}
