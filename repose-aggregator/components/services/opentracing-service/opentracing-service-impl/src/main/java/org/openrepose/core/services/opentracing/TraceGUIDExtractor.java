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
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * TraceGUIDExtractor - implements TextMap to allow for span value extraction from Repose trans-id header
 *
 * TODO: remove this once opentracing-contrib/repose jar is published
 * For more information, see {@link io.opentracing.Tracer#extract(Format, Object)}
 */
public class TraceGUIDExtractor implements TextMap {
    private static final Logger LOG = LoggerFactory.getLogger(TraceGUIDExtractor.class);

    public Map<String, String> getTracingMap() {
        return tracingMap;
    }

    private Map<String, String> tracingMap;

    public TraceGUIDExtractor(String tracingHeaderValue) throws IOException{
        LOG.trace("Extract header from request wrapper");
        if (StringUtils.isBlank(tracingHeaderValue)) {
             LOG.trace("header does not exist.  Bail out here");
             this.tracingMap = new HashMap<>();
        } else {
            LOG.trace("Check if it's base64");
            if (Base64.isBase64(tracingHeaderValue)) {
                LOG.trace("Decode it");
                ObjectMapper mapper = new ObjectMapper();
                try {
                    this.tracingMap = mapper.readValue(
                        Base64.decodeBase64(tracingHeaderValue), new TypeReference<Map<String, String>>() {
                        });
                } catch (IOException ioe) {
                    LOG.warn("Unable to extract value from tracing header: {} due to {}",
                            tracingHeaderValue, ioe.getLocalizedMessage());
                    ioe.printStackTrace();
                    this.tracingMap = new HashMap<>();
                }
            } else {
                LOG.warn("Provided tracing data is not base64 encoded: {}", tracingHeaderValue);
                this.tracingMap = new HashMap<>();
            }
        }
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        // given an x-trans-id json, iterate through its key/value pairs
        return this.tracingMap.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("This class should be used only with Tracer.inject()!");
    }

}
