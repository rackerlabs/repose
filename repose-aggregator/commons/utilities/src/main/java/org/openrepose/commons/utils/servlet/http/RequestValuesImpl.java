/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.commons.utils.servlet.http;

import javax.servlet.http.HttpServletRequest;

public class RequestValuesImpl implements RequestValues {

    private static final String REQUEST_URI_ATTRIBUTE = "repose.request.uri";
    private static final String REQUEST_URL_ATTRIBUTE = "repose.request.url";
    private final HttpServletRequest request;
    private final HeaderValues headers;
    private final RequestDestinations destinations;
    private final RequestQueryParameters parameters;

    public RequestValuesImpl(HttpServletRequest request) {
        this.request = request;
        this.headers = HeaderValuesImpl.extract(request);
        this.destinations = new RequestDestinationsImpl(request);
        this.parameters = new RequestQueryParametersImpl(request);
        request.setAttribute(REQUEST_URI_ATTRIBUTE, request.getRequestURI());
        request.setAttribute(REQUEST_URL_ATTRIBUTE, request.getRequestURL());
    }

    @Override
    public String getRequestURI() {
        return (String) request.getAttribute(REQUEST_URI_ATTRIBUTE);
    }

    @Override
    public void setRequestURI(String uri) {
        request.setAttribute(REQUEST_URI_ATTRIBUTE, uri);
    }

    @Override
    public StringBuffer getRequestURL() {
        return (StringBuffer) request.getAttribute(REQUEST_URL_ATTRIBUTE);
    }

    @Override
    public void setRequestURL(StringBuffer url) {
        request.setAttribute(REQUEST_URL_ATTRIBUTE, url);
    }

    @Override
    public RequestDestinations getDestinations() {
        return destinations;
    }

    @Override
    public HeaderValues getHeaders() {
        return headers;
    }

    @Override
    public RequestQueryParameters getQueryParameters() {
        return parameters;
    }
}
