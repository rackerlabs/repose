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
package org.openrepose.commons.utils.test.mocks.providers;

import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBException;
import java.io.IOException;

public class MockServiceProvider {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MockServiceProvider.class);

    public String getEchoBody(HttpServletRequest request, String body) {

        StringBuilder resp = new StringBuilder("");
        try {
            resp = resp.append(RequestUtil.servletRequestToXml(request, body));
        } catch (IOException | JAXBException e) {
            LOG.trace("", e);
        }

        return resp.toString();

    }

    public Response getEndService(HttpServletRequest request, String body) {
        return getEndService("200", request, body);
    }

    public Response getEndService(HttpServletRequest request) {
        return getEndService("200", request);
    }

    public Response getEndService(String statusCode, HttpServletRequest request) {

        return getEndService(statusCode, request, "");
    }

    public Response getEndService(String statusCode, HttpServletRequest request, String body) {
        int status;
        try {
            status = Integer.parseInt(statusCode);
        } catch (NumberFormatException e) {

            status = Response.Status.NOT_FOUND.getStatusCode();
        }

        String resp = getEchoBody(request, body);

        ResponseBuilder response = Response.status(status);

        return response.entity(resp).header("x-request-id", "somevalue").header("Content-Length", resp.length()).build();
    }

}
