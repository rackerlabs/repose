package com.rackspace.papi.mocks;

import com.rackspace.papi.mocks.providers.MockServiceProvider;

import java.io.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * @author malconis
 */
@Path("/")
public class MockServiceResource {

    private MockServiceProvider provider;

    public MockServiceResource() {
        provider = new MockServiceProvider();
    }
    
    @DELETE
    @Path("/deleteme")
    public Response delete(String body, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
        return echoBody(body == null || body.trim().isEmpty()? "no body": body, headers, uriInfo);
    }
    
    
    
    @POST
    @Path("/emptyresponsebody")
    public Response emptyResponseBody(String body, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
        Response.ResponseBuilder response = Response.ok();
        return response.build();
    }
    

    @POST
    @Path("/echobody")
    public Response echoBodyRoot(String body, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
        return echoBody(body, headers, uriInfo);
    }

    @POST
    @Path("{prefix: .*}/echobody")
    public Response echoBody(String body, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
        Response.ResponseBuilder response = Response.ok();
        String type = headers.getRequestHeader("content-type").isEmpty() ? "" : headers.getRequestHeader("content-type").get(0);
        if (type.length() > 0) {
            response = response.type(type);
        }
        return response.entity(body).header("Content-Length", body.length()).build();
    }

    @PUT
    @Path("/echobody")
    public Response echoBodyRootPut(String body, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
        return echoBody(body, headers, uriInfo);
    }

    @PUT
    @Path("{prefix: .*}/echobody")
    public Response echoBodyPut(String body, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
        Response.ResponseBuilder response = Response.ok();
        String type = headers.getRequestHeader("content-type").isEmpty() ? "" : headers.getRequestHeader("content-type").get(0);
        if (type.length() > 0) {
            response = response.type(type);
        }
        return response.entity(body).header("Content-Length", body.length()).build();
    }

    @POST
    @Path("/postcode/{statusCode}")
    public Response postStatusCode(@PathParam("statusCode") String statusCode, String body, @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        URI uri = uriInfo.getAbsolutePath();

        return provider.postStatusCode(body, statusCode, uri.toURL().toExternalForm().replaceAll("/statuscode/", "/"), headers, uriInfo, request);
    }

    @GET
    @Path("/statuscode/{statusCode}")
    public Response getStatusCode(@PathParam("statusCode") String statusCode, @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        URI uri = uriInfo.getAbsolutePath();

        return provider.getStatusCode(statusCode, uri.toURL().toExternalForm().replaceAll("/statuscode/", "/"), headers, uriInfo, request);
    }

    @GET
    @Path("/location/{statusCode}")
    public Response getLocation(@PathParam("statusCode") String statusCode, @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        URI uri = uriInfo.getAbsolutePath();

        return provider.getLocation(statusCode, uri.toURL().toExternalForm(), headers, uriInfo, request);
    }

    @GET
    @Path("/rawstatuscode/{statusCode}")
    public Response getRawStatusCode(@PathParam("statusCode") String statusCode, @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        URI uri = uriInfo.getAbsolutePath();

        return provider.getRawStatusCode(statusCode, uri.toURL().toExternalForm().replaceAll("/statuscode/", "/"), headers, uriInfo, request);
    }

    @GET
    @Path("/echoheaders")
    public Response getEchoHeaders(@Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        return provider.getEndServiceWithEchoHeaders("", headers, uriInfo, request);
    }

    @GET
    @Path("/echoheaders/{suffix: .*}")
    public Response getEchoHeadersWithPath(@Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest request) throws MalformedURLException, URISyntaxException {
        return provider.getEndServiceWithEchoHeaders("", headers, uriInfo, request);
    }

    @GET
    @Path("{id : .*}")
    public Response getEndService(@Context HttpHeaders headers, @Context UriInfo uri, @Context HttpServletRequest request) {

        return provider.getEndService("", headers, uri, request);
    }

    @GET
    @Path("/")
    public Response getService(@Context HttpHeaders headers, @Context UriInfo uri, @Context HttpServletRequest request) {
        return provider.getEndService("", headers, uri, request);
    }

    private StreamingOutput streamBytes(final byte[] bytes) {
        return new StreamingOutput() {

            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                for (byte b : bytes) {
                    out.write(b);
                }

                out.flush();
                out.close();
            }
        };

    }

    @GET
    @Path("/stream{extra: .*}")
    public StreamingOutput getStreamingService(@Context HttpHeaders headers, @Context UriInfo uri, @Context HttpServletRequest request) {
        final String body = provider.getEchoBody("", headers, uri, request);
        return streamBytes(body.getBytes());
    }

    @POST
    @Path("/post-stream{extra: .*}")
    public StreamingOutput getPostStreamingService(String body, @Context HttpHeaders headers, @Context UriInfo uri, @Context HttpServletRequest request) {
        final String data = provider.getEchoBody(body, headers, uri, request);
        return streamBytes(data.getBytes());
    }
    

    

    @GET
    @Path("/responsesize/{size}")
    public StreamingOutput getSizedResponse(final @PathParam("size") int size, @Context HttpHeaders headers, @Context UriInfo uri) {
        return new StreamingOutput() {

            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                BufferedOutputStream buff = new BufferedOutputStream(out);

                for (int i = 0; i < size; i++) {
                    buff.write((byte) (i % 128));
                }

                buff.flush();
                buff.close();
            }
        };
    }

    @GET
    @Path("{prefix: .*}/responsesize/{size}")
    public StreamingOutput getSizedResponse2(final @PathParam("size") int size, @Context HttpHeaders headers, @Context UriInfo uri) {
        return getSizedResponse(size, headers, uri);
    }

    /*
    @GET
    @Path("{prefix1: .*}/{prefix2: .*}/responsesize/{size}")
    public StreamingOutput getSizedResponse3(final @PathParam("size") int size, @Context HttpHeaders headers, @Context UriInfo uri) {
       return getSizedResponse(size, headers, uri);
    }

    @GET
    @Path("{prefix1: .*}/{prefix2: .*}/{prefix3: .*}/responsesize/{size}")
    public StreamingOutput getSizedResponse4(final @PathParam("size") int size, @Context HttpHeaders headers, @Context UriInfo uri) {
       return getSizedResponse(size, headers, uri);
    }
    *
    */


    @POST
    @Path("{id : .*}")
    public Response postEndService(String body, @Context HttpHeaders headers, @Context UriInfo uri, @Context HttpServletRequest request) {
        return provider.getEndService(body, headers, uri, request);
    }

    @POST
    @Path("/")
    public Response postService(String body, @Context HttpHeaders headers, @Context UriInfo uri, @Context HttpServletRequest request) {
        return provider.getEndService(body, headers, uri, request);
    }

    @GET
    @Path("/{service}/limits")
    @Produces("application/json")
    public Response getLimitsJson() {
        return provider.getAbsoluteLimitsJSON();
    }

    @GET
    @Path("/{service}/limits")
    @Produces("application/xml")
    public Response getLimitsXml() {
        return provider.getAbsoluteLimitsXML();
    }

    @GET
    @Path("/{version}/{user}/limits")
    @Produces("application/json")
    public Response getAbsoluteLimitsJson() {
        return provider.getAbsoluteLimitsJSON();

    }

    @GET
    @Path("/{version}/{user}/limits")
    @Produces("application/xml")
    public Response getAbsoluteLimits() {
        return provider.getAbsoluteLimitsXML();
    }

    @GET
    @Path("/delayedresponse/{time}")
    public Response getDelayedResponse(@PathParam("time") int time, @Context HttpHeaders headers, @Context UriInfo uri, @Context HttpServletRequest request) {
        return provider.getDelayedResponse(time, headers, uri, request);
    }

    @GET
    @Path("/nova/limits")
    public Response getNovaLimits() {

        StringBuilder limits = new StringBuilder();

        limits.append("<limits xmlns:atom=\"http://www.w3.org/2005/Atom\"");
        limits.append(" xmlns=\"http://docs.openstack.org/common/api/v1.1\"><rates/><absolute><limit name=\"maxServerMeta\"");
        limits.append(" value=\"5\"/><limit name=\"maxPersonality\" value=\"5\"/><limit name=\"maxImageMeta\" value=\"5\"/><limit name=\"maxPersonalitySize\"");
        limits.append(" value=\"1000\"/><limit name=\"maxTotalInstances\" value=\"1000\"/><limit name=\"maxTotalRAMSize\" value=\"10240000\"/></absolute></limits>");

        return Response.ok(limits.toString()).build();
    }

    @GET
    @Path("/loadbalancers/absolutelimits")
    @Produces("application/xml")
    public Response getLBaaSLimitsXml() {

        return provider.getLBaaSLimitsXml();
    }

    @GET
    @Path("/loadbalancers/absolutelimits")
    @Produces("application/json")
    public Response getLBaaSLimitsJson() {

        return provider.getLBaaSLimitsJson();
    }

    @GET
    @Path("/{user}/loadbalancers/absolutelimits")
    @Produces("application/xml")
    public Response getLBaaSLimitsXmlUser() {

        return provider.getLBaaSLimitsXml();
    }

    @GET
    @Path("/{user}/loadbalancers/absolutelimits")
    @Produces("application/json")
    public Response getLBaaSLimitsJsonUser() {

        return provider.getLBaaSLimitsJson();
    }

    @GET
    @Path("/whatismyip")
    public Response getRequestingIp(@Context HttpServletRequest req) {

        return provider.getRequestingIp(req);
    }
}
