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

import org.openrepose.filters.ratelimiting.util.LimitsEntityStreamTransformer;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class LimitsResponseMimeTypeWriter {

    /*
     LinkedHashSet was chosen to demonstrate that duplicate values are not allowed, and that ordering matters.
     The order of the LinkedHashSet represents our preference; we would rather return media types that appear
     earlier in the list than those that appear later.
     */
    public static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
            // TODO: Add support for MediaType.TEXT_XML
    )));

    private final LimitsEntityStreamTransformer responseTransformer;

    public LimitsResponseMimeTypeWriter(LimitsEntityStreamTransformer responseTransformer) {
        this.responseTransformer = responseTransformer;
    }

    public MediaType writeLimitsResponse(byte[] readableContents, MediaType mediaType, OutputStream outputStream) throws IOException {
        if (MediaType.APPLICATION_XML_VALUE.equalsIgnoreCase(mediaType.toString())) {
            outputStream.write(readableContents);
            return MediaType.APPLICATION_XML;
        } else {
            // Default to JSON
            responseTransformer.streamAsJson(new ByteArrayInputStream(readableContents), outputStream);
            return MediaType.APPLICATION_JSON;
        }
    }
}
