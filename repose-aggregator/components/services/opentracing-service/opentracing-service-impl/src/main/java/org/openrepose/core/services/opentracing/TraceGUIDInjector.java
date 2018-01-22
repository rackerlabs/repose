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
package org.openrepose.core.services.opentracing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * TraceGUIDInjector - implements TextMap to allow for span injection into x-trans-id header
 *
 * TODO: remove this once opentracing-contrib/repose jar is published
 * For more information, see {@link io.opentracing.Tracer#inject(SpanContext, Format, Object)}
 */
public class TraceGUIDInjector implements TextMap {
    private HttpServletRequestWrapper httpServletRequestWrapper;
    private String tracerHeader;
    private Map<String, String> headers;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TraceGUIDInjector.class);

    public HttpServletRequestWrapper getHttpServletRequestWrapper() {
        return httpServletRequestWrapper;
    }

    public String getTracerHeader() {
        return tracerHeader;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Repose specific tracer injector.  Uses Repose's HttpServletRequestWrapper
     * @param httpServletRequestWrapper {@link HttpServletRequestWrapper object}
     * @param tracerHeader String representing Repose's tracing header.
     */
    public TraceGUIDInjector(HttpServletRequestWrapper httpServletRequestWrapper, String tracerHeader) {
        this.httpServletRequestWrapper = httpServletRequestWrapper;
        this.tracerHeader = tracerHeader;

    }

    /**
     * More common tracer injector.  Uses passed in headers.
     * @param headers {@link Map that stores http request headers}
     * @param tracerHeader String representing Repose's tracing header.
     */
    public TraceGUIDInjector(Map<String, String> headers, String tracerHeader) {
        this.headers = headers;
        this.tracerHeader = tracerHeader;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("Should be used only with tracer#inject()");
    }

    @Override
    public void put(String key, String value) {
        LOG.trace("Add the span {}=>{}", key, value);

        if (StringUtils.isBlank(key) || StringUtils.isBlank(value))
            LOG.warn("Span key or value was not provided.  Unable to inject span information.");
        else if (this.tracerHeader == null)
            LOG.warn("Tracing header was not provided.  Unable to inject span information.");
        else {
            // check if it is a Repose specific implementation
            if (this.httpServletRequestWrapper != null) {
                String tracerValueWithSpan = extractAndEncodeTracingValue(
                        this.httpServletRequestWrapper.getHeader(this.tracerHeader), key, value);

                if (StringUtils.isNotBlank(tracerValueWithSpan))
                    this.httpServletRequestWrapper.replaceHeader(this.tracerHeader, tracerValueWithSpan);

            } else if (this.headers != null) {
                String tracerValueWithSpan = extractAndEncodeTracingValue(
                        this.headers.get(this.tracerHeader), key, value);

                if (StringUtils.isNotBlank(tracerValueWithSpan))
                    this.headers.put(this.tracerHeader, tracerValueWithSpan);
            } else {
                LOG.warn("Unable to inject value into tracing header.  Both request wrapper and headers are null");
            }
        }
    }

    private String extractAndEncodeTracingValue(String tracerValue, String key, String value) {
        if (StringUtils.isBlank(tracerValue) || !Base64.isBase64(tracerValue)) {
            LOG.trace("Tracer header was not provided.  Make a new one");
        } else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, String> tracingMap = mapper.readValue(
                        Base64.decodeBase64(tracerValue), new TypeReference<Map<String, String>>() {
                        });

                tracingMap.put(key, value);

                LOG.trace("Add header to the request wrapper {} {}",
                        this.tracerHeader, mapper.writeValueAsBytes(tracingMap));

                return new String(
                        Base64.encodeBase64(mapper.writeValueAsBytes(tracingMap)));
            } catch (IOException ioe) {
                LOG.warn("Unable to extract value from tracing header: {}", tracerValue);
            }
        }

        return null;

    }
}
