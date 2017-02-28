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
package org.openrepose.commons.utils.http.media;

import org.openrepose.commons.utils.http.header.HeaderValue;

import java.util.ArrayList;
import java.util.List;

public class MediaRangeProcessor {
    private final List<? extends HeaderValue> values;

    public MediaRangeProcessor(List<? extends HeaderValue> values) {
        this.values = values;
    }

    public List<MediaType> process() {
        List<MediaType> result = new ArrayList<>();

        for (HeaderValue value : values) {
            result.add(process(value));
        }

        return result;
    }

    public MediaType process(HeaderValue headerValue) {
        String mediaTypeWithParametersStripped = headerValue.getValue().split(";")[0];

        MimeType mediaType = MimeType.getMatchingMimeType(mediaTypeWithParametersStripped);

        if (MimeType.UNKNOWN.equals(mediaType)) {
            mediaType = MimeType.guessMediaTypeFromString(mediaTypeWithParametersStripped);
        }

        return new MediaType(mediaTypeWithParametersStripped, mediaType, headerValue.getParameters());
    }
}
