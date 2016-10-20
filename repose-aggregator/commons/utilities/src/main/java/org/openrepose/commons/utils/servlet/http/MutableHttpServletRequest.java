/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.io.BufferedServletInputStream;
import org.openrepose.commons.utils.io.ByteBufferInputStream;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.io.buffer.ByteBuffer;
import org.openrepose.commons.utils.io.buffer.CyclicByteBuffer;
import org.openrepose.commons.utils.io.stream.LimitedReadInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author jhopper
 */
// This class is non-final so that we can mock it in unit tests.  We cannot
// mock classes that are marked as final.
@SuppressWarnings("com.puppycrawl.tools.checkstyle.checks.design.FinalClassCheck")
public class MutableHttpServletRequest extends HttpServletRequestWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(MutableHttpServletRequest.class);
    private static final String REQUEST_ID = "requestId";
    private final RequestValues values;
    private final long streamLimit;
    private ServletInputStream inputStream;
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

    public static MutableHttpServletRequest wrap(HttpServletRequest request) {
        return request instanceof MutableHttpServletRequest ? (MutableHttpServletRequest) request : new MutableHttpServletRequest(request);
    }

    public static MutableHttpServletRequest wrap(HttpServletRequest request, long streamLimit) {
        return request instanceof MutableHttpServletRequest ? (MutableHttpServletRequest) request : new MutableHttpServletRequest(request, streamLimit);
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

    public long getStreamLimit() {
        return streamLimit;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {

        //Making a terrible horrible assumption that we have mark and reset because of http mutable stupidity.
        //When we fix the servlet spec we need to rewrite all the filters and this to make it work.
        synchronized (this) {
            if (inputStream == null) {

                if (streamLimit <= 0) {
                    inputStream = new BufferedServletInputStream(super.getInputStream());
                } else {
                    inputStream = new BufferedServletInputStream(new LimitedReadInputStream(streamLimit, super.getInputStream()));
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

    /**
     * Returns the size of the content body by reading through the input stream.
     * WARNING: This will cause some performance degradation as the request body will be read and
     * not just streamed through repose.
     *
     * @return Size of content body based off of content within the request servletinputstream
     * @throws IOException
     */
    public int getRealBodyLength() throws IOException {

        synchronized (this) {

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            RawInputStreamReader.instance().copyTo(getInputStream(), baos);

            if (baos.toByteArray().length > 0) {
                final ByteBuffer internalBuffer = new CyclicByteBuffer(baos.toByteArray().length, true);
                internalBuffer.put(baos.toByteArray());
                this.setInputStream(new ByteBufferInputStream(internalBuffer));
            }

            return baos.toByteArray().length;

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

    @Override
    public String getContentType() {
        Enumeration<String> contentTypeHeaders = getHeaders(HttpHeaders.CONTENT_TYPE);
        String contentType = null;

        if (contentTypeHeaders.hasMoreElements()) {
            contentType = contentTypeHeaders.nextElement();
            if (contentTypeHeaders.hasMoreElements()) {
                LOG.warn("Multiple values found in the Content-Type header.");
            }
        }

        return contentType;
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

    public RequestValues getRequestValues() {
        return values;
    }
}
