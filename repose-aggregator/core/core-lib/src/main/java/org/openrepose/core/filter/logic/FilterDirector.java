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
package org.openrepose.core.filter.logic;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.systemmodel.Destination;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * TODO: Starting to feel like a candidate for a bit of ISP love - read below for more info.
 * <p/>
 * Feels like there's three separate domains being represented: filter direction
 * (routing, action, application), response modification (response headers,
 * response writer, response status code, body), and lastly request modification
 * (request url and uri, query parameters, request header). I didn't think these domains were too
 * different early on but now that we need to communicate more directives, the
 * domains have begun to diverge.
 */
public interface FilterDirector {

    /**
     * Status code (-1) indicates that Repose does not support
     * the action/response requested/received.
     */
    public static final int SC_UNSUPPORTED_RESPONSE_CODE = -1;

    /**
     * Status code (429) indicates that the user has sent too many
     * requests in a given amount of time ("rate limiting").
     */
    public static final int SC_TOO_MANY_REQUESTS = 429;

    void setRequestUriQuery(String query);

    String getRequestUri();

    void setRequestUri(String newUri);

    StringBuffer getRequestUrl();

    void setRequestUrl(StringBuffer newUrl);

    HeaderManager requestHeaderManager();

    HeaderManager responseHeaderManager();

    FilterAction getFilterAction();

    /**
     * Informs the Filter Chain whether to continue (PASS), stop and return immediately (RETURN), or continue and then
     * handle the response on the unwind (PROCESS_RESPONSE).
     * The default is NOT_SET and shouldn't be used. This should always be set to one of the other three.
     *
     * @param action the action to take
     */
    void setFilterAction(FilterAction action);

    int getResponseStatusCode();

    void setResponseStatusCode(int status);

    String getResponseMessageBody();

    byte[] getResponseMessageBodyBytes();

    PrintWriter getResponseWriter();

    OutputStream getResponseOutputStream();

    void applyTo(MutableHttpServletRequest request);

    void applyTo(MutableHttpServletResponse response) throws IOException;

    RouteDestination addDestination(String id, String uri, double quality);

    RouteDestination addDestination(Destination dest, String uri, double quality);

    List<RouteDestination> getDestinations();
}
