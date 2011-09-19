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
        final String header = fromMap(headers, name.toLowerCase());

        return header != null ? header : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return combine(super.getHeaderNames(), headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return combine(super.getHeaders(name), headers.get(name));
    }

    public <T> Enumeration<T> combine(Enumeration<T> enumeration, Collection<T>... extraElements) {
        final List<T> combinedValues = new LinkedList<T>();

        while (enumeration.hasMoreElements()) {
            combinedValues.add(enumeration.nextElement());
        }
        
        for (Collection<T> collection : extraElements) {
            if (collection != null && !collection.isEmpty()) {
                combinedValues.addAll(collection);
            }
        }
        
        return Collections.enumeration(combinedValues);
    }

    static String fromMap(Map<String, List<String>> headers, String headerName) {
        final List<String> headerValues = headers.get(headerName);

        return (headerValues != null && headerValues.size() > 0) ? headerValues.get(0) : null;
    }
}
