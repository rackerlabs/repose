package com.rackspace.papi.commons.util.servlet.http;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public final class MutableHttpServletRequest extends HttpServletRequestWrapper {

    public static MutableHttpServletRequest wrap(HttpServletRequest request) {
        return request instanceof MutableHttpServletRequest ? (MutableHttpServletRequest) request : new MutableHttpServletRequest(request);
    }
    
    private final Map<String, List<String>> headers;
    private StringBuffer requestUrl;
    private String requestUri;

    private MutableHttpServletRequest(HttpServletRequest request) {
        super(request);

        requestUrl = request.getRequestURL();
        requestUri = request.getRequestURI();

        headers = new HashMap<String, List<String>>();
        
        copyHeaders(request);
    }
    
    private void copyHeaders(HttpServletRequest request) {
        final Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement().toLowerCase();  //Normalize to lowercase
            
            final Enumeration<String> headerValues = request.getHeaders(headerName);
            final List<String> copiedHeaderValues = new LinkedList<String>();
            
            while (headerValues.hasMoreElements()) {
                copiedHeaderValues.add(headerValues.nextElement());
            }
            
            headers.put(headerName, copiedHeaderValues);
        }
    }

    @Override
    public String getRequestURI() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
        return requestUrl;
    }

    public void setRequestUrl(StringBuffer requestUrl) {
        this.requestUrl = requestUrl;
    }

    public void addHeader(String name, String value) {
        final String lowerCaseName = name.toLowerCase();

        List<String> headerValues = headers.get(lowerCaseName);

        if (headerValues == null) {
            headerValues = new LinkedList<String>();
        }

        headerValues.add(value);

        headers.put(lowerCaseName, headerValues);
    }

    public void replaceHeader(String name, String value) {
        final List<String> headerValues = new LinkedList<String>();

        headerValues.add(value);

        headers.put(name.toLowerCase(), headerValues);
    }

    public void removeHeader(String name) {
        headers.remove(name.toLowerCase());
    }

    @Override
    public String getHeader(String name) {
        return fromMap(headers, name.toLowerCase());
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        final List<String> headerValues = headers.get(name.toLowerCase());
        
        return Collections.enumeration(headerValues != null ? headerValues : Collections.EMPTY_SET);
    }

    static String fromMap(Map<String, List<String>> headers, String headerName) {
        final List<String> headerValues = headers.get(headerName);

        return (headerValues != null && headerValues.size() > 0) ? headerValues.get(0) : null;
    }
}
