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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.openrepose.core.systemmodel.config.ChunkedEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.http.HttpHeaders.TRANSFER_ENCODING;
import static org.apache.http.protocol.HTTP.CHUNK_CODING;

/**
 * Translates a servlet request to an HTTP client request by copying over header values, query string parameters, and
 * request body (if present).
 */
public class HttpComponentRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(HttpComponentRequestProcessor.class);
    private static final Set<String> EXCLUDE_HEADERS = Stream.of(
        "connection",
        "content-length",
        "expect",
        "transfer-encoding").collect(Collectors.toSet());

    // todo: By specification, only the TRACE method MUST NOT have a body. For other methods, body semantics
    // todo: are not defined. Consider allowing bodies for non-TRACE methods.
    // todo: https://tools.ietf.org/html/rfc7231#section-4.3.8
    private static final Set<String> NO_ENTITY_METHODS = Stream.of(
        HttpGet.METHOD_NAME,
        HttpHead.METHOD_NAME,
        HttpOptions.METHOD_NAME,
        HttpTrace.METHOD_NAME).collect(Collectors.toSet());

    private final HttpServletRequest servletRequest;
    private final URI target;
    private final boolean rewriteHostHeader;
    private final ChunkedEncoding chunkedEncoding;

    private static boolean excludeHeader(String header) {
        return EXCLUDE_HEADERS.contains(header.toLowerCase());
    }

    public HttpComponentRequestProcessor(HttpServletRequest servletRequest,
                                  URI target,
                                  boolean rewriteHostHeader,
                                  ChunkedEncoding chunkedEncoding) {
        this.servletRequest = servletRequest;
        this.target = target;
        this.rewriteHostHeader = rewriteHostHeader;
        this.chunkedEncoding = chunkedEncoding;
    }

    /**
     * Performs the translation between request representations.
     * <p>
     * Note that calling this method may cause the servlet request body to be read and consumed.
     *
     * @return a request to be used with an HTTP client
     * @throws URISyntaxException
     * @throws IOException
     */
    public HttpUriRequest process() throws URISyntaxException, IOException {
        final RequestBuilder requestBuilder = RequestBuilder.create(servletRequest.getMethod())
            .setUri(getUri());
        if (!NO_ENTITY_METHODS.contains(servletRequest.getMethod())) {
            requestBuilder.setEntity(getEntity());
        }
        final HttpUriRequest clientRequest = requestBuilder.build();
        setHeaders(clientRequest);
        return clientRequest;
    }

    private URI getUri() throws URISyntaxException {
        URIBuilder builder = new URIBuilder(target.toString() + servletRequest.getRequestURI());
        String queryString = servletRequest.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            builder.setQuery(queryString);
            if (builder.getQueryParams().isEmpty()) {
                builder.removeQuery();
            }
        }
        return builder.build();
    }

    /**
     * Scan header values and manipulate as necessary. Host header, if provided, may need to be updated.
     */
    private String processHeaderValue(String headerName, String headerValue) {
        String result = headerValue;

        // todo: is this necessary, or redundant with setting the URI?
        // In case the proxy host is running multiple virtual servers,
        // rewrite the Host header to ensure that we get content from
        // the correct virtual server
        if (rewriteHostHeader && headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
            if (target.getPort() == -1) {
                result = target.getHost();
            } else {
                result = target.getHost() + ":" + target.getPort();
            }
        }

        return result;
    }

    /**
     * Copy header values from the servlet request to the http message.
     */
    private void setHeaders(HttpMessage message) {
        final Enumeration<String> headerNames = servletRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();

            if (!excludeHeader(headerName)) {
                final Enumeration<String> headerValues = servletRequest.getHeaders(headerName);

                while (headerValues.hasMoreElements()) {
                    String headerValue = headerValues.nextElement();
                    message.addHeader(headerName, processHeaderValue(headerName, headerValue));
                }
            }
        }
    }

    /*
     * todo: Replace new-ing entities with Apache's EntityBuilder if/when it supports not setting the
     * todo: content-type on an entity.
     * todo: Content-Type is not a required HTTP header, and we do not want to add it if the original request
     * todo: did not contain it.
     * todo: Instead, we rely on setting the Content-Type header to convey the Content-Type.
     */
    private HttpEntity getEntity() throws IOException {
        HttpEntity httpEntity = new InputStreamEntity(servletRequest.getInputStream(), -1);
        switch (chunkedEncoding) {
            case TRUE:
                break;
            case AUTO:
                if (StringUtils.equalsIgnoreCase(servletRequest.getHeader(TRANSFER_ENCODING), CHUNK_CODING)) {
                    break;
                }
            case FALSE:
                httpEntity = new ByteArrayEntity(IOUtils.toByteArray(servletRequest.getInputStream()));
                break;
            default:
                LOG.warn("Invalid chunked encoding value -- using chunked encoding");
                break;
        }
        return httpEntity;
    }
}
