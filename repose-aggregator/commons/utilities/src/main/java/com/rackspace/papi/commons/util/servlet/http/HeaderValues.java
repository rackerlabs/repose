package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderName;
import com.rackspace.papi.commons.util.http.header.HeaderValue;

import java.util.Enumeration;
import java.util.List;

public interface HeaderValues {

    void addHeader(String name, String value);

    void addDateHeader(String name, long value);
    
    String getHeader(String name);
    
    HeaderValue getHeaderValue(String name);

    /**
     * @deprecated  {@link #getHeaderNamesAsList() getHeaderNamesAsList} should be used instead
     */
    @Deprecated
    Enumeration<String> getHeaderNames();

    List<HeaderName> getHeaderNamesAsList();

    /**
     * @deprecated  {@link #getHeaderValues(String) getHeaderValues} should be used instead
     */
    @Deprecated
    Enumeration<String> getHeaders(String name);
    
    List<HeaderValue> getHeaderValues(String name);

    List<HeaderValue> getPreferredHeaders(String name, HeaderValue defaultValue);

    List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue);

    void removeHeader(String name);

    void replaceHeader(String name, String value);
    
    void replaceDateHeader(String name, long value);
    
    void clearHeaders();
    
    boolean containsHeader(String name);

}
