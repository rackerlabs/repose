package com.rackspace.papi.external.testing.mocks;

import com.rackspace.papi.external.testing.mocks.providers.MockServiceProvider;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/")
public class MockServiceResource {

    private static final String DEFAULT_RESPONSE_CODE = "200";
    private MockServiceProvider provider;

    public MockServiceResource() {
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
