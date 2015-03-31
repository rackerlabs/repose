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
package org.openrepose.filters.versioning.util.http;

import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.servlet.RequestMediaRangeInterrogator;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// NOTE: This does not belong in util - this is a domain object for versioning only
public class HttpRequestInfoImpl implements HttpRequestInfo {

    private final List<MediaType> acceptMediaRange;
    private final MediaType preferedMediaRange;
    private final String uri;
    private final String url;
    private final String host;
    private final String scheme;
    public HttpRequestInfoImpl(HttpServletRequest request) {
        this(getMediaRanges(request), request.getRequestURI(), request.getRequestURL().toString(), request.getHeader(CommonHttpHeader.HOST.toString()), request.getScheme());
    }

    public HttpRequestInfoImpl(List<MediaType> acceptMediaRange, String uri, String url, String host, String scheme) {
        this.preferedMediaRange = acceptMediaRange.get(0);
        this.acceptMediaRange = acceptMediaRange;
        this.uri = uri;
        this.url = url;
        this.host = host;
        this.scheme = scheme;
    }

    private static List<MediaType> getMediaRanges(HttpServletRequest request) {
        MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
        List<HeaderValue> preferredAcceptHeader = mutableRequest.getPreferredHeaderValues(CommonHttpHeader.ACCEPT.toString());
        return RequestMediaRangeInterrogator.interrogate(request.getRequestURI(), preferredAcceptHeader);
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public MediaType getPreferedMediaRange() {
        return preferedMediaRange;
    }

    @Override
    public boolean hasMediaRange(MediaType targetRange) {
        for (MediaType requestedRange : acceptMediaRange) {
            if (requestedRange.equals(targetRange)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getScheme() {
        return scheme;
    }
}
