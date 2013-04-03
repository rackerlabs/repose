package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.io.BufferedServletInputStream;
import com.rackspace.papi.commons.util.io.stream.LimitedReadInputStream;
import com.rackspace.papi.commons.util.io.stream.ServletInputStreamWrapper;
import java.io.IOException;
import java.util.*;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 *
 * @author jhopper
 */
// This class is non-final so that we can mock it in unit tests.  We cannot
// mock classes that are marked as final.
@SuppressWarnings("com.puppycrawl.tools.checkstyle.checks.design.FinalClassCheck")
public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    public static MutableHttpServletRequest wrap(HttpServletRequest request) {
        return request instanceof MutableHttpServletRequest ? (MutableHttpServletRequest) request : new MutableHttpServletRequest(request);
    }

    public static MutableHttpServletRequest wrap(HttpServletRequest request, long streamLimit) {
        return request instanceof MutableHttpServletRequest ? (MutableHttpServletRequest) request : new MutableHttpServletRequest(request, streamLimit);
    }
    private static final String REQUEST_ID = "requestId";
    private ServletInputStream inputStream;
    private final RequestValues values;
    private final long streamLimit;

    private MutableHttpServletRequest(HttpServletRequest request) {
      this(request, -1);
    }

    private MutableHttpServletRequest(HttpServletRequest request, long streamLimit) {
        super(request);

        if (getAttribute(REQUEST_ID) == null) {
          setAttribute(REQUEST_ID, UUID.randomUUID().toString());
        }
        this.values = new RequestValuesImpl(request);
        this.streamLimit = streamLimit;
    }
    
    public String getRequestId() {
      return (String) getAttribute(REQUEST_ID);
    }

    public void addDestination(String id, String uri, float quality) {
        addDestination(new RouteDestination(id, uri, quality));
    }

    public void addDestination(RouteDestination dest) {
        values.getDestinations().addDestination(dest);
    }

    public RouteDestination getDestination() {
        return values.getDestinations().getDestination();
    }

    @Override
    public String getQueryString() {
        return values.getQueryParameters().getQueryString();
    }

    public void setQueryString(String requestUriQuery) {
        values.getQueryParameters().setQueryString(requestUriQuery);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return values.getQueryParameters().getParameterNames();
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return values.getQueryParameters().getParameterMap();
    }

    @Override
    public String getParameter(String name) {
        return values.getQueryParameters().getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name) {
        return values.getQueryParameters().getParameterValues(name);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        synchronized (this) {
            if (inputStream == null) {

                if (streamLimit <= 0) {
                    inputStream = new BufferedServletInputStream(super.getInputStream());
                } else {
                    inputStream = new ServletInputStreamWrapper(new LimitedReadInputStream(streamLimit, super.getInputStream()));
                }
            }
            return inputStream;
        }
    }

    public void setInputStream(ServletInputStream inputStream) {
        synchronized (this) {
            this.inputStream = inputStream;
        }
    }

    @Override
    public String getRequestURI() {
        return values.getRequestURI();
    }

    public void setRequestUri(String requestUri) {
        values.setRequestURI(requestUri);
    }

    @Override
    public StringBuffer getRequestURL() {
        return values.getRequestURL();
    }

    public void setRequestUrl(StringBuffer requestUrl) {
        values.setRequestURL(requestUrl);
    }
    
    public void clearHeaders() {
      values.getHeaders().clearHeaders();
    }

    public void addHeader(String name, String value) {
        values.getHeaders().addHeader(name, value);
    }

    public void replaceHeader(String name, String value) {
        values.getHeaders().replaceHeader(name, value);
    }

    public void removeHeader(String name) {
        values.getHeaders().removeHeader(name);
    }

    @Override
    public String getHeader(String name) {
        return values.getHeaders().getHeader(name);
    }

    public HeaderValue getHeaderValue(String name) {
        return values.getHeaders().getHeaderValue(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return values.getHeaders().getHeaderNames();
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return values.getHeaders().getHeaders(name);
    }

    public HeaderValue getPreferredHeader(String name) {
        return getPreferredHeader(name, null);
    }

    public HeaderValue getPreferredHeader(String name, HeaderValue defaultValue) {
        List<HeaderValue> headerValues = values.getHeaders().getPreferredHeaderValues(name, defaultValue);

        return !headerValues.isEmpty() ? headerValues.get(0) : null;
    }

    public List<HeaderValue> getPreferredHeaderValues(String name) {
        return values.getHeaders().getPreferredHeaderValues(name, null);
    }

    public List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue) {
        return values.getHeaders().getPreferredHeaderValues(name, defaultValue);
    }

    // Method to retrieve a list of a specified headers values
    // Order of values are determined first by quality then by order as they were passed to the request.
    public List<HeaderValue> getPreferredHeaders(String name, HeaderValue defaultValue) {
        return values.getHeaders().getPreferredHeaders(name, defaultValue);
    }
}
