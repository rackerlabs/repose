package com.rackspace.papi.mocks;

import com.rackspace.papi.mocks.providers.MockServiceProvider;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author malconis
 */
@Path("/")
public class MockServiceResource {

   private MockServiceProvider provider;

   public MockServiceResource() {
      provider = new MockServiceProvider();
   }

   @POST
   @Path("/postcode/{statusCode}")
   public Response postStatusCode(@PathParam("statusCode") String statusCode, String body, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
      URI uri = uriInfo.getAbsolutePath();

      return provider.postStatusCode(statusCode, uri.toURL().toExternalForm().replaceAll("/statuscode/", "/"), headers, uriInfo);
   }

   @GET
   @Path("/statuscode/{statusCode}")
   public Response getStatusCode(@PathParam("statusCode") String statusCode, @Context HttpHeaders headers, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
      URI uri = uriInfo.getAbsolutePath();

      return provider.getStatusCode(statusCode, uri.toURL().toExternalForm().replaceAll("/statuscode/", "/"), headers, uriInfo);
   }

   @GET
   @Path("{id : .*}")
   public Response getEndService(@Context HttpHeaders headers, @Context UriInfo uri) {
      return provider.getEndService(new String(), headers, uri);
   }

   @GET
   @Path("/")
   public Response getService(@Context HttpHeaders headers, @Context UriInfo uri) {
      return provider.getEndService(new String(), headers, uri);
   }

   @POST
   @Path("{id : .*}")
   public Response postEndService(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
      return provider.getEndService(body, headers, uri);
   }

   @POST
   @Path("/")
   public Response postService(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
      return provider.getEndService(body, headers, uri);
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
   public Response getDelayedResponse(@PathParam("time") int time, @Context HttpHeaders headers, @Context UriInfo uri) {
      return provider.getDelayedResponse(time, headers, uri);
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
