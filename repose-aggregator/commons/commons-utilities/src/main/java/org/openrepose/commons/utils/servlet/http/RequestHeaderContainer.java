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

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class RequestHeaderContainer implements HeaderContainer {

    private final HttpServletRequest request;
    private final List<HeaderName> headerNames;
    private final Map<HeaderName, List<HeaderValue>> headerValues;


    public RequestHeaderContainer(HttpServletRequest request) {
        this.request = request;
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<HeaderName> extractHeaderNames() {
        List<HeaderName> result = new LinkedList<>();
        if (request != null) {
            Enumeration<String> names = request.getHeaderNames();

            if (names != null) {
                while (names.hasMoreElements()) {
                    result.add(HeaderName.wrap(names.nextElement()));
                }
            }
        }

        return result;
    }

    private Map<HeaderName, List<HeaderValue>> extractHeaderValues() {
        Map<HeaderName, List<HeaderValue>> valueMap = new HashMap<>();

        if (request != null) {
            for (HeaderName wrappedName : getHeaderNames()) {
                valueMap.put(wrappedName, extractValues(wrappedName));
            }
        }

        return valueMap;
    }

    private List<HeaderValue> extractValues(HeaderName name) {

        List<HeaderValue> values = new ArrayList<>();

        Enumeration<String> vals = request.getHeaders(name.getName());

        while (vals.hasMoreElements()) {
            values.add(new HeaderValueImpl(vals.nextElement()));
        }

        return values;

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
        return HeaderContainerType.REQUEST;
    }
}
