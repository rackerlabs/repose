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
package org.openrepose.filters.ratelimiting.write;

import org.openrepose.core.services.ratelimit.config.Limits;
import org.openrepose.core.services.ratelimit.config.RateLimitList;
import org.openrepose.filters.ratelimiting.exception.RateLimitingSerializationException;
import org.openrepose.filters.ratelimiting.util.LimitsEntityStreamTransformer;
import org.slf4j.Logger;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class ActiveLimitsWriter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ActiveLimitsWriter.class);
    private static final LimitsEntityStreamTransformer RESPONSE_TRANSFORMER = new LimitsEntityStreamTransformer();

    public MediaType write(RateLimitList activeRateLimits, MediaType mediaType, OutputStream outputStream) {

        try {
            final Limits limits = new Limits();
            limits.setRates(activeRateLimits);

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            RESPONSE_TRANSFORMER.entityAsXml(limits, bos);

            final LimitsResponseMimeTypeWriter responseWriter = new LimitsResponseMimeTypeWriter(RESPONSE_TRANSFORMER);

            return responseWriter.writeLimitsResponse(bos.toByteArray(), mediaType, outputStream);
        } catch (Exception ex) {
            LOG.error("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
            throw new RateLimitingSerializationException("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
        }
    }
}
