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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LimitsResponseMimeTypeWriter {

    public static final Set<MediaType> SUPPORTED_MEDIA_TYPES = Stream.of(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
            // TODO: Add support for MediaType.TEXT_XML
    ).collect(Collectors.toSet());

    private final LimitsEntityStreamTransformer responseTransformer;

    public LimitsResponseMimeTypeWriter(LimitsEntityStreamTransformer responseTransformer) {
        this.responseTransformer = responseTransformer;
    }

    public MediaType writeLimitsResponse(byte[] readableContents, MediaType mediaType, OutputStream outputStream) throws IOException {
        /*
         Check for JSON first since it is the preferred media type.
         This is done explicitly even though JSON is also the default.
          */
        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            responseTransformer.streamAsJson(new ByteArrayInputStream(readableContents), outputStream);
            return MediaType.APPLICATION_JSON;
        } else if (MediaType.APPLICATION_XML.isCompatibleWith(mediaType)) {
            outputStream.write(readableContents);
            return MediaType.APPLICATION_XML;
        } else {
            // default to json for now
            responseTransformer.streamAsJson(new ByteArrayInputStream(readableContents), outputStream);
            return MediaType.APPLICATION_JSON;
        }
    }
}
