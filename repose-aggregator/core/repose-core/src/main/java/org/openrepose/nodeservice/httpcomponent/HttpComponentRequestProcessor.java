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
package org.openrepose.nodeservice.httpcomponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.io.BufferedServletInputStream;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.proxy.common.AbstractRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 */
class HttpComponentRequestProcessor extends AbstractRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(HttpComponentRequestProcessor.class);

    private final boolean rewriteHostHeader;
    private final URI targetHost;

    private HttpServletRequest sourceRequest;
    private String chunkedEncoding;

    public HttpComponentRequestProcessor(HttpServletRequest request, URI host, boolean rewriteHostHeader,
                                         String chunkedEncoding) {
        this.sourceRequest = request;
        this.targetHost = host;
        this.rewriteHostHeader = rewriteHostHeader;
        this.chunkedEncoding = chunkedEncoding;
    }

    private void setQueryString(URIBuilder builder) throws URISyntaxException {
        String queryString = sourceRequest.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            builder.setQuery(queryString);
            if (builder.getQueryParams().isEmpty()) {
                builder.removeQuery();
            }
        }
    }

    public URI getUri(String target) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(target);
        setQueryString(builder);
        return builder.build();
    }

    /**
     * Scan header values and manipulate as necessary. Host header, if provided, may need to be updated.
     *
     * @param headerName
     * @param headerValue
     * @return
     */
    private String processHeaderValue(String headerName, String headerValue) {
        String result = headerValue;

        // In case the proxy host is running multiple virtual servers,
        // rewrite the Host header to ensure that we get content from
        // the correct virtual server
        if (rewriteHostHeader && headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
            result = targetHost.getHost() + ":" + targetHost.getPort();
        }

        return result;
    }

    /**
     * Copy header values from source request to the http method.
     *
     * @param method
     */
    private void setHeaders(HttpRequestBase method) {
        final Enumeration<String> headerNames = sourceRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();

            if (excludeHeader(headerName)) {
                continue;
            }

            final Enumeration<String> headerValues = sourceRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                method.addHeader(headerName, processHeaderValue(headerName, headerValue));
            }
        }
    }

    /**
     * Process a base http request. Base http methods will not contain a message body.
     *
     * @param method
     * @return
     */
    public HttpRequestBase process(HttpRequestBase method) {
        setHeaders(method);
        return method;
    }

    /**
     * Process an entity enclosing http method. These methods can handle a request body.
     *
     * @param method
     * @return
     * @throws IOException
     */
    // todo: remove the need for a synchronized method -- this is the only method that needs to be synchronized for now
    //       since it modifies the sourceRequest
    public synchronized HttpRequestBase process(HttpEntityEnclosingRequestBase method) throws IOException {
        final int contentLength = getEntityLength();
        setHeaders(method);
        method.setEntity(new InputStreamEntity(sourceRequest.getInputStream(), contentLength));
        return method;
    }

    private int getEntityLength() throws IOException {
        // Default to -1, which will be treated as an unknown entity length leading to the usage of chunked encoding.
        int entityLength = -1;
        switch (chunkedEncoding.toLowerCase()) {
            case "true":
                break;
            case "auto":
                if (StringUtils.equalsIgnoreCase(sourceRequest.getHeader("transfer-encoding"), "chunked")) {
                    break;
                }
            case "false":
                entityLength = getSizeOfRequestBody();
                break;
            default:
                LOG.warn("Invalid chunked encoding value -- using chunked encoding");
                break;
        }
        return entityLength;
    }

    private int getSizeOfRequestBody() throws IOException {
        // todo: optimize so subsequent calls to this method do not need to read/copy the entity
        final ByteArrayOutputStream sourceEntity = new ByteArrayOutputStream();
        RawInputStreamReader.instance().copyTo(sourceRequest.getInputStream(), sourceEntity);

        final ServletInputStream readableEntity = new BufferedServletInputStream(new ByteArrayInputStream(sourceEntity.toByteArray()));
        sourceRequest = new HttpServletRequestWrapper(sourceRequest, readableEntity);

        return sourceEntity.size();
    }
}
