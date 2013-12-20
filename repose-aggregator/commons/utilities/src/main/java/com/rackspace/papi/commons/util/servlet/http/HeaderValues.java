package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import java.util.Enumeration;
import java.util.List;

public interface HeaderValues {

    void addHeader(String name, String value);

    void addDateHeader(String name, long value);
    
    String getHeader(String name);
    
    HeaderValue getHeaderValue(String name);

    Enumeration<String> getHeaderNames();

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
