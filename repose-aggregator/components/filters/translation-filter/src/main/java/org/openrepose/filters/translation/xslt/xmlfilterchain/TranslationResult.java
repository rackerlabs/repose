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
package org.openrepose.filters.translation.xslt.xmlfilterchain;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.docs.repose.httpx.v1.*;
import org.openrepose.filters.translation.httpx.HttpxMarshaller;
import org.openrepose.filters.translation.xslt.XsltParameter;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TranslationResult {

    public static final String HEADERS_OUTPUT = "headers.xml";
    public static final String QUERY_OUTPUT = "query.xml";
    public static final String REQUEST_OUTPUT = "request.xml";
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationResult.class);
    private final boolean success;
    private final List<XsltParameter<? extends OutputStream>> outputs;
    private final HttpxMarshaller marshaller;

    TranslationResult(boolean success) {
        this(success, null);
    }

    TranslationResult(boolean success, List<XsltParameter<? extends OutputStream>> outputs) {
        this.success = success;
        this.outputs = outputs;
        this.marshaller = new HttpxMarshaller();
    }

    public boolean isSuccess() {
        return success;
    }

    private <T extends OutputStream> T getStream(String name) {
        if (outputs == null) {
            return null;
        }

        for (XsltParameter<? extends OutputStream> output : outputs) {
            if (name.equalsIgnoreCase(output.getName())) {
                return (T) output.getValue();
            }
        }

        return null;
    }

    public <T extends OutputStream> T getRequestInfoStream() {
        return getStream(REQUEST_OUTPUT);
    }

    public <T extends OutputStream> T getHeadersStream() {
        return getStream(HEADERS_OUTPUT);
    }

    public <T extends OutputStream> T getParams() {
        return getStream(QUERY_OUTPUT);
    }

    public void applyResults(final HttpServletRequestWrapper request, final HttpServletResponseWrapper response) {
        applyHeaders(request, response);
        applyQueryParams(request);
        applyRequestInfo(request);
    }

    public RequestInformation getRequestInfo() {
        ByteArrayOutputStream requestOutput = getRequestInfoStream();

        if (requestOutput == null) {
            return null;
        }

        byte[] requestBytes = requestOutput.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(requestBytes);
        if (input.available() == 0) {
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("New request info: " + new String(requestBytes, StandardCharsets.UTF_8));
        }

        return marshaller.unmarshallRequestInformation(input);
    }

    private void applyRequestInfo(final HttpServletRequestWrapper request) {
        RequestInformation requestInfo = getRequestInfo();

        if (requestInfo == null) {
            return;
        }

        if (StringUtils.isNotBlank(requestInfo.getUri())) {
            request.setRequestURI(requestInfo.getUri());
        }

        if (StringUtils.isNotBlank(requestInfo.getUrl())) {
            try {
                URL url = new URL(requestInfo.getUrl());
                if (StringUtils.isNotBlank(url.getProtocol())) {
                    request.setScheme(url.getProtocol());
                }
                if (StringUtils.isNotBlank(url.getHost())) {
                    request.setServerName(url.getHost());
                }
                if (url.getPort() > 0) {
                    request.setServerPort(url.getPort());
                }
                if (StringUtils.isNotBlank(url.getPath())) {
                    request.setRequestURI(url.getPath());
                }
                if (StringUtils.isNotBlank(url.getQuery())) {
                    request.setQueryString(url.getQuery());
                }
            } catch (MalformedURLException e) {
                LOG.info("Failed to parse the translated URL: {}", requestInfo.getUrl());
                LOG.trace("", e);
            }
        }
    }

    public QueryParameters getQueryParameters() {
        ByteArrayOutputStream paramsOutput = getParams();

        if (paramsOutput == null) {
            return null;
        }

        ByteArrayInputStream input = new ByteArrayInputStream(paramsOutput.toByteArray());
        if (input.available() == 0) {
            return null;
        }

        return marshaller.unmarshallQueryParameters(input);

    }

    private void applyQueryParams(final HttpServletRequestWrapper request) {
        QueryParameters params = getQueryParameters();

        if (params == null) {
            return;
        }

        if (params.getParameter() != null) {
            StringBuilder sb = new StringBuilder();

            for (NameValuePair param : params.getParameter()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }

                sb.append(param.getName()).append("=").append(param.getValue() != null ? param.getValue() : "");
            }

            request.setQueryString(sb.toString());
        }
    }

    public Headers getHeaders() {
        ByteArrayOutputStream headersOutput = getHeadersStream();
        if (headersOutput == null) {
            return null;
        }

        byte[] out = headersOutput.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(out);
        if (input.available() == 0) {
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("New headers: " + new String(out, StandardCharsets.UTF_8));
        }

        return marshaller.unmarshallHeaders(input);
    }

    private void applyHeaders(final HttpServletRequestWrapper request, final HttpServletResponseWrapper response) {
        Headers headers = getHeaders();

        if (headers == null) {
            return;
        }

        if (headers.getRequest() != null) {
            for (String header : request.getHeaderNamesList()) {
                request.removeHeader(header);
            }

            for (QualityNameValuePair header : headers.getRequest().getHeader()) {
                if (header.getQuality() == 1.0) {
                    request.appendHeader(header.getName(), header.getValue());
                } else {
                    request.appendHeader(header.getName(), header.getValue(), header.getQuality());
                }
            }
        }

        if (headers.getResponse() != null) {
            for (String header : response.getHeaderNames()) {
                response.removeHeader(header);
            }

            for (QualityNameValuePair header : headers.getResponse().getHeader()) {
                if (header.getQuality() == 1.0) {
                    response.appendHeader(header.getName(), header.getValue());
                } else {
                    response.appendHeader(header.getName(), header.getValue(), header.getQuality());
                }
            }
        }
    }
}
