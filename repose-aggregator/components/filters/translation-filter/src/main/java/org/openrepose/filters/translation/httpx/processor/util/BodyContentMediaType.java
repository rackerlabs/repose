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
package org.openrepose.filters.translation.httpx.processor.util;

import java.util.regex.Pattern;

public enum BodyContentMediaType {

    // matches strings like application/xml or application/vendor.xml.somestring;param=1
    XML("(?i)[^/]+/(?:[^;]+[\\.\\+])*" + BodyContentMediaType.XML_VALUE + "(?:[\\.\\+][^;]+)*(?:$|;.*)"),
    // matches strings like application/json or application/vendor.json.somestring;param=1
    JSON("(?i)[^/]+/(?:[^;]+[\\.\\+])*" + BodyContentMediaType.JSON_VALUE + "(?:[\\.\\+][^;]+)*(?:$|;.*)"),
    UNKNOWN(".*");
    private static final String JSON_VALUE = "json";
    private static final String XML_VALUE = "xml";
    private final Pattern pattern;

    BodyContentMediaType(String regex) {
        pattern = Pattern.compile(regex);
    }

    public static BodyContentMediaType getMediaType(String contentType) {
        if (contentType != null) {
            for (BodyContentMediaType mediaType : values()) {
                if (mediaType.getPattern().matcher(contentType).matches()) {
                    return mediaType;
                }
            }
        }

        return UNKNOWN;
    }

    Pattern getPattern() {
        return pattern;
    }
}
