package com.rackspace.papi.mocks;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.rackspace.papi.mocks.providers.MockServiceProvider;

/**
 * @author malconis
 */
@Path("/")
public class MockServiceResource {

   private MockServiceProvider provider;

   public MockServiceResource() {
      provider = new MockServiceProvider();
   }

   @GET
   @Path("{id : .*}")
   public Response getEndService(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
      return provider.getEndService(body, headers, uri);
   }

   @GET
   @Path("/")
   public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
      return provider.getEndService(body, headers, uri);
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
   @Path("*/statuscode/{statusCode}")
   public Response getStatusCode(@PathParam("statusCode") String statusCode) {
      return provider.getStatusCode(statusCode);
   }

   @GET
   @Path("*/delayedresponse/{time}")
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

      StringBuilder limits = new StringBuilder();

      limits.append("<limits xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\">");
      limits.append("<absolute>");
      limits.append("<limit name=\"IPV6_LIMIT\" value=\"25\"/>");
      limits.append("<limit name=\"LOADBALANCER_LIMIT\" value=\"25\"/>");
      limits.append("<limit name=\"BATCH_DELETE_LIMIT\" value=\"10\"/>");
      limits.append("<limit name=\"ACCESS_LIST_LIMIT\" value=\"100\"/>");
      limits.append("<limit name=\"NODE_LIMIT\" value=\"25\"/>");
      limits.append("<absolute>");
      limits.append("</limits>");

      return Response.ok(limits.toString()).build();
   }

   @GET
   @Path("/loadbalancers/absolutelimits")
   @Produces("application/json")
   public Response getLBaaSLimitsJson() {

      StringBuilder limits = new StringBuilder();

      limits.append("{");
      limits.append("\"absolute\":");
      limits.append("[");
      limits.append("{\"name\":\"IPV6_LIMIT\",\"value\":25},");
      limits.append("{\"name\":\"LOADBALANCER_LIMIT\",\"value\":25},");
      limits.append("{\"name\":\"BATCH_DELETE_LIMIT\",\"value\":10},");
      limits.append("{\"name\":\"ACCESS_LIST_LIMIT\",\"value\":100},");
      limits.append("{\"name\":\"NODE_LIMIT\",\"value\":25}");
      limits.append("]");
      limits.append("}");      

      return Response.ok(limits.toString()).build();
   }

}
