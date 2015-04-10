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
package org.openrepose.commons.utils.http.header;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zinic
 */
public class HeaderFieldParser {

    private final List<String> headerValueStrings;
    private Pattern date = Pattern.compile("[^\\d]{3},\\s*[\\d]{2}\\s*[^\\d]{3}\\s*[\\d]{4}\\s*[\\d]{2}:[\\d]{2}:[\\d]{2}\\s*GMT");

    private HeaderFieldParser() {
        headerValueStrings = new LinkedList<String>();
    }

    public HeaderFieldParser(String rawHeaderString) {
        this();

        if (rawHeaderString != null) {
            addValue(rawHeaderString);
        }
    }

    public HeaderFieldParser(String rawHeaderString, String headerName) {
        this();

        if (rawHeaderString != null) {
            addValue(rawHeaderString, headerName);
        }
    }

    public HeaderFieldParser(Enumeration<String> headerValueEnumeration) {
        this();

        if (headerValueEnumeration != null) {
            while (headerValueEnumeration.hasMoreElements()) {
                addValue(headerValueEnumeration.nextElement());
            }
        }
    }

    public HeaderFieldParser(Enumeration<String> headerValueEnumeration, String headerName) {
        this();

        if (headerValueEnumeration != null) {
            while (headerValueEnumeration.hasMoreElements()) {
                addValue(headerValueEnumeration.nextElement(), headerName);
            }
        }
    }


    public HeaderFieldParser(Collection<String> headers) {
        this();

        if (headers != null) {
            for (String header : headers) {
                addValue(header);
            }
        }
    }

    public HeaderFieldParser(Collection<String> headers, String headerName) {
        this();

        if (headers != null) {
            for (String header : headers) {
                addValue(header, headerName);
            }
        }
    }

    private void addValue(String rawHeaderString) {
        Matcher matcher = date.matcher(rawHeaderString);
        if (matcher.matches()) {
            // This is an RFC 1123 date string
            headerValueStrings.add(rawHeaderString);
            return;
        }

        final String[] splitHeaderValues = rawHeaderString.split(",");

        for (String splitHeaderValue : splitHeaderValues) {
            if (!splitHeaderValue.isEmpty()) {
                headerValueStrings.add(splitHeaderValue.trim());
            }
        }
    }

    private void addValue(String rawHeaderString, String headerName) {
        Matcher matcher = date.matcher(rawHeaderString);
        if (matcher.matches()) {
            // This is an RFC 1123 date string
            headerValueStrings.add(rawHeaderString);
            return;
        }

        if (!rawHeaderString.isEmpty()) {
            headerValueStrings.add(rawHeaderString.trim());
        }
    }

    public List<HeaderValue> parse() {
        final List<HeaderValue> headerValues = new LinkedList<HeaderValue>();

        for (String headerValueString : headerValueStrings) {
            headerValues.add(new HeaderValueParser(headerValueString).parse());
        }

        return headerValues;
    }

    public Pattern getDate() {
        return date;
    }

}
