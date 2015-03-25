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
package org.openrepose.commons.utils.test.mocks;

import org.openrepose.commons.utils.test.mocks.providers.MockServiceProvider;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/")
public class MocksServletResource {

    private static final String DEFAULT_RESPONSE_CODE = "200";
    private MockServiceProvider provider;

    public MocksServletResource() {
        provider = new MockServiceProvider();
    }


    @GET
    @Path("{id : .*}")
    public Response getEndService(@Context HttpServletRequest request) {
        return provider.getEndService(request);
    }

    @GET
    @Path("/")
    public Response getService(@Context HttpServletRequest request) {
        return provider.getEndService(request);
    }


    @POST
    @Path("/")
    public Response postService(String body, @Context HttpServletRequest request) {
        return provider.getEndService(DEFAULT_RESPONSE_CODE, request, body);
    }

    @POST
    @Path("{id : .*}")
    public Response postEndService(String body, @Context HttpServletRequest request) {

        return provider.getEndService(DEFAULT_RESPONSE_CODE, request, body);
    }

}
