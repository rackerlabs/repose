package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import java.util.Enumeration;
import java.util.List;

public interface RequestHeaderValues {

    void addHeader(String name, String value);

    String getHeader(String name);

    Enumeration<String> getHeaderNames();

    Enumeration<String> getHeaders(String name);

    List<HeaderValue> getPreferredHeaders(String name, HeaderValue defaultValue);

    List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue);

    void removeHeader(String name);

    void replaceHeader(String name, String value);
}
