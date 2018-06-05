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
package org.openrepose.commons.utils.http;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.message.BasicHeaderValueParser;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dan Daley
 */
public class ServiceClientResponse {

    private final InputStream data;
    private final int statusCode;
    private final Header[] headers;

    public ServiceClientResponse(int code, InputStream data) {
        this.statusCode = code;
        this.headers = new Header[0];
        this.data = data;
    }

    public ServiceClientResponse(int code, Header[] headers, InputStream data) {
        this.statusCode = code;
        this.headers = headers;
        this.data = data;
    }

    public InputStream getData() {
        return data;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public List<String> getHeaders(String headerName) {
        return Arrays.stream(headers)
            .filter(h -> h.getName().equalsIgnoreCase(headerName))
            .map(Header::getValue)
            .collect(Collectors.toList());
    }

    public List<HeaderElement> getHeaderElements(String headerName) {
        return getHeaders(headerName).stream()
            .flatMap(value -> Arrays.stream(BasicHeaderValueParser.parseElements(value, null)))
            .collect(Collectors.toList());
    }

    public int getStatus() {
        return statusCode;
    }
}
