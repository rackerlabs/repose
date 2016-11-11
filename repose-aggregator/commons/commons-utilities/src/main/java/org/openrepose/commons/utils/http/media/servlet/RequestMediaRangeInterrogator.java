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
package org.openrepose.commons.utils.http.media.servlet;

import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.header.HeaderValueParser;
import org.openrepose.commons.utils.http.media.MediaRangeProcessor;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.http.media.VariantParser;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class RequestMediaRangeInterrogator {

    private RequestMediaRangeInterrogator() {
    }

    public static List<MediaType> interrogate(String requestUri, List<String> acceptHeaderValues) {
        final List<MediaType> ranges = new LinkedList<>();

        final MimeType mediaType = VariantParser.getMediaTypeFromVariant(requestUri);

        if (mediaType == null) {
            List<HeaderValue> convertedValues = acceptHeaderValues.stream()
                    .map(headerValue -> new HeaderValueParser(headerValue).parse())
                    .collect(Collectors.toList());

            ranges.addAll(new MediaRangeProcessor(convertedValues).process());
        } else {
            ranges.add(new MediaType(mediaType.getName(), mediaType, 1));
        }

        if (ranges.isEmpty()) {
            ranges.add(new MediaType(MimeType.UNSPECIFIED.getName(), MimeType.UNSPECIFIED, -1));
        }

        return ranges;
    }
}
