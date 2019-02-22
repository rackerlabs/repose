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
package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.header.HeaderValueImpl;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class ResponseHeaderContainer implements HeaderContainer {

    private final HttpServletResponse response;
    private final List<HeaderName> headerNames;
    private final Map<HeaderName, List<HeaderValue>> headerValues;

    public ResponseHeaderContainer(HttpServletResponse response) {
        this.response = response;
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<HeaderName> extractHeaderNames() {
        List<HeaderName> result = new LinkedList<>();
        if (response != null) {
            Collection<String> names = response.getHeaderNames();

            for (String name : names) {
                result.add(HeaderName.wrap(name));
            }
        }

        return result;
    }

    private Map<HeaderName, List<HeaderValue>> extractHeaderValues() {
        Map<HeaderName, List<HeaderValue>> valueMap = new HashMap<>();

        if (response != null) {
            for (HeaderName headerNameKey : headerNames) {
                String name = headerNameKey.getName();

                List<HeaderValue> values = new ArrayList<>();
                values.add(new HeaderValueImpl(response.getHeader(name)));
                valueMap.put(headerNameKey, values);
            }
        }

        return valueMap;
    }

    @Override
    public List<HeaderName> getHeaderNames() {
        return headerNames;
    }

    @Override
    public List<HeaderValue> getHeaderValues(String name) {
        return headerValues.get(HeaderName.wrap(name));
    }

    @Override
    public boolean containsHeader(String name) {
        List<HeaderValue> values = getHeaderValues(name);
        return values != null && !values.isEmpty();
    }

    @Override
    public HeaderContainerType getContainerType() {
        return HeaderContainerType.RESPONSE;
    }
}
