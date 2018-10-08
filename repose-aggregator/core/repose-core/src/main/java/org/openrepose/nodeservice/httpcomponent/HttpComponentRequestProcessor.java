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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.openrepose.commons.utils.io.BufferedServletInputStream;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.proxy.common.AbstractRequestProcessor;
import org.openrepose.core.systemmodel.config.ChunkedEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import static org.apache.http.HttpHeaders.TRANSFER_ENCODING;
import static org.apache.http.protocol.HTTP.CHUNK_CODING;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 */
class HttpComponentRequestProcessor extends AbstractRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(HttpComponentRequestProcessor.class);

    private final boolean rewriteHostHeader;
    private final URI targetHost;

    private HttpServletRequest sourceRequest;
    private ChunkedEncoding chunkedEncoding;

    public HttpComponentRequestProcessor(HttpServletRequest request, URI host, boolean rewriteHostHeader,
                                         ChunkedEncoding chunkedEncoding) {
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
    private void setHeaders(HttpUriRequest method) {
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
     * Process an http method. These methods can handle a request body.
     *
     * @param method
     * @return
     * @throws IOException
     */
    // todo: remove the need for a synchronized method -- this is the only method that needs to be synchronized for now
    public synchronized HttpUriRequest process(HttpUriRequest method) throws IOException {
        setHeaders(method);

        if (method instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase) method).setEntity(getEntity(sourceRequest.getInputStream()));
        }

        return method;
    }

    /*
     * todo: Replace new-ing entities with Apache's EntityBuilder if/when it supports not setting the
     * todo: content-type on an entity.
     * todo: Content-Type is not a required HTTP header, and we do not want to add it if the original request
     * todo: did not contain it.
     * todo: Instead, we rely on setting the Content-Type header to convey the Content-Type.
     *
     * private HttpEntity getEntity(InputStream sourceStream) throws IOException {
     *     final EntityBuilder entityBuilder = EntityBuilder.create()
     *         .setStream(sourceStream);
     *     switch (chunkedEncoding) {
     *         case TRUE:
     *             break;
     *         case AUTO:
     *             if (StringUtils.equalsIgnoreCase(sourceRequest.getHeader(TRANSFER_ENCODING), CHUNK_CODING)) {
     *                 break;
     *             }
     *         case FALSE:
     *             entityBuilder.setBinary(readSourceStream());
     *             break;
     *         default:
     *             LOG.warn("Invalid chunked encoding value -- using chunked encoding");
     *             break;
     *     }
     *     return entityBuilder.build();
     * }
     */
    private HttpEntity getEntity(InputStream sourceStream) throws IOException {
        HttpEntity httpEntity = new InputStreamEntity(sourceStream, -1);
        switch (chunkedEncoding) {
            case TRUE:
                break;
            case AUTO:
                if (StringUtils.equalsIgnoreCase(sourceRequest.getHeader(TRANSFER_ENCODING), CHUNK_CODING)) {
                    break;
                }
            case FALSE:
                httpEntity = new ByteArrayEntity(readSourceStream());
                break;
            default:
                LOG.warn("Invalid chunked encoding value -- using chunked encoding");
                break;
        }
        return httpEntity;
    }

    private byte[] readSourceStream() throws IOException {
        // todo: optimize so subsequent calls to this method do not need to read/copy the entity
        final ByteArrayOutputStream sourceEntity = new ByteArrayOutputStream();
        RawInputStreamReader.instance().copyTo(sourceRequest.getInputStream(), sourceEntity);

        final ServletInputStream readableEntity = new BufferedServletInputStream(new ByteArrayInputStream(sourceEntity.toByteArray()));
        sourceRequest = new HttpServletRequestWrapper(sourceRequest, readableEntity);

        return sourceEntity.toByteArray();
    }
}
