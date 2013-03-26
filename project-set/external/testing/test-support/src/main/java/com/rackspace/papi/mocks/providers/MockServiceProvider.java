package com.rackspace.papi.mocks.providers;

import com.rackspace.repose.service.limits.schema.AbsoluteLimit;
import com.rackspace.repose.service.limits.schema.AbsoluteLimitList;
import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.limits.schema.ObjectFactory;
import com.rackspace.papi.components.ratelimit.util.LimitsEntityTransformer;
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

public class MockServiceProvider {

   private ObjectFactory factory;
   private String[] absNames = {"Admin", "Tech", "Demo"};
   private static final int THREE_HUNDRED = 300;
   private static final int FOUR_HUNDRED = 400;
   private static final String NEW_LOCATION_HEADER = "X-NEW-LOCATION";

   public MockServiceProvider() {
      factory = new ObjectFactory();
   }

   public String getEchoBody(String body, HttpHeaders headers, UriInfo uri, HttpServletRequest request) {
      Set<String> headerPairs = headers.getRequestHeaders().keySet();
      Set<String> queryParams = uri.getQueryParameters().keySet();
      StringBuilder resp = new StringBuilder("<html>\n\t<head>\n\t\t<title>Servlet version</title>\n\t</head>\n\t<body>\n\t\t<h1>Servlet version at ");
      resp.append(uri.getPath()).append("</h1>");
      UriBuilder absolutePathBuilder = uri.getAbsolutePathBuilder();

      resp.append("<h1>Base URI: ").append(uri.getBaseUri().toString()).append("</h1>\r\n");
      resp.append("<h2>Absolute URI: ").append(absolutePathBuilder.build().toString()).append("</h2>\r\n");
      resp.append("<h2>Requested URL: ").append(request.getRequestURL().toString()).append("</h2>\r\n");

      try {
         resp.append("<h3>Server : ").append(InetAddress.getLocalHost().getHostAddress()).append("</h3>");
      } catch (UnknownHostException ex) {
         resp.append("<h3>Server : ").append("Unknown").append("</h3>");
      }

      List<String> header;
      if (!headerPairs.isEmpty()) {
         resp.append("\n\t\t<h2>HEADERS</h2>");
         for (String h : headerPairs) {
            header = headers.getRequestHeader(h);
            for (String hh : header) {
               resp.append("\n\t\t<h3> ").append(h).append(" : ").append(hh).append("</h3>");
            }
         }
      }
      if (!queryParams.isEmpty()) {
         resp.append("\n\t\t<h2>Query Parameters</h2>");
         resp.append("\n\t\t<h3>").append(uri.getRequestUri().getQuery()).append("</h3>");
         for (String q : queryParams) {
            resp.append("\n\t\t<h3> ").append(q).append(" : ").append(uri.getQueryParameters().get(q)).append("</h3>");
         }
      }
      if (!body.isEmpty()) {
         resp.append("\n\t\t<h2>Body</h2>");
         resp.append("\n\t\t\t<h3>").append(body).append("</h3>");

      }

      resp.append("\n\t</body>\n</html>");

      return resp.toString();
   }

   public Response getEndService(String body, HttpHeaders headers, UriInfo uri, HttpServletRequest request) {
      return getEndService(body, "200", headers, uri, request);
   }

   public Response getEndService(String body, String statusCode, HttpHeaders headers, UriInfo uri, HttpServletRequest request) {
      int status;
      try {
         status = Integer.parseInt(statusCode);
      } catch (NumberFormatException e) {

         status = Response.Status.NOT_FOUND.getStatusCode();
      }


      String resp = getEchoBody(body, headers, uri, request);

      ResponseBuilder response = Response.status(status);

      return response.entity(resp).header("x-request-id", "somevalue").header("Content-Length", resp.length()).build();
   }

   public Response getEndServiceWithEchoHeaders(String body, HttpHeaders headers, UriInfo uri, HttpServletRequest request) {
      String resp = getEchoBody(body, headers, uri, request);

      ResponseBuilder response = Response.ok();
      for (String headerName : headers.getRequestHeaders().keySet()) {
         for (String headerValue : headers.getRequestHeader(headerName)) {
            response.header(headerName, headerValue);
         }
      }

      return response.entity(resp).header("x-request-id", "somevalue").header("Content-Length", resp.length()).build();
   }

   public Response getAbsoluteLimitsJSON() {

      Limits limits = new Limits();
      AbsoluteLimitList absList = buildAbsoluteLimits();
      limits.setAbsolute(absList);

      LimitsEntityTransformer transformer = new LimitsEntityTransformer();
      return Response.ok(transformer.entityAsJson(limits)).build();
   }

   public Response getAbsoluteLimitsXML() {

      Limits limits = new Limits();
      AbsoluteLimitList absList = buildAbsoluteLimits();
      limits.setAbsolute(absList);

      return Response.ok(factory.createLimits(limits)).build();
   }

   private AbsoluteLimitList buildAbsoluteLimits() {
      AbsoluteLimitList limitList = new AbsoluteLimitList();


      AbsoluteLimit abs;
      int value = 20;
      for (String name : absNames) {
         abs = new AbsoluteLimit();
         abs.setName(name);
         abs.setValue(value -= 5);
         limitList.getLimit().add(abs);
      }

      return limitList;
   }

   public Response getStatusCode(String statusCode, String location, HttpHeaders headers, UriInfo uri, HttpServletRequest request) throws URISyntaxException {

      int status;
      try {
         status = Integer.parseInt(statusCode);
      } catch (NumberFormatException e) {
         status = Response.Status.NOT_FOUND.getStatusCode();
      }

      String resp = getEchoBody("", headers, uri, request);

      if (status >= THREE_HUNDRED && status < FOUR_HUNDRED) {

         URI newLocation = new URI(location);

         return Response.status(status).header("Location", newLocation).entity(resp).build();
      }

      if (status == Response.Status.UNAUTHORIZED.getStatusCode()) {
         final com.rackspacecloud.docs.auth.api.v1.ObjectFactory of = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

         UnauthorizedFault fault = new UnauthorizedFault();
         fault.setCode(Response.Status.UNAUTHORIZED.getStatusCode());
         fault.setMessage("Beware!  You are unauthorized.");
         return Response.ok(of.createUnauthorized(fault)).status(Response.Status.UNAUTHORIZED).build();
      }

      return Response.status(status).entity(resp).build();

   }

   public Response getLocation(String statusCode, String location, HttpHeaders headers, UriInfo uri, HttpServletRequest request) throws URISyntaxException {

      int status;
      try {
         status = Integer.parseInt(statusCode);
      } catch (NumberFormatException e) {
         status = Response.Status.NOT_FOUND.getStatusCode();
      }

      List<String> newLocations = headers.getRequestHeader(NEW_LOCATION_HEADER);

      URI newLocation = new URI(location.replaceAll("", ""));
      if (newLocations != null && newLocations.size() > 0) {
         newLocation = new URI(newLocations.get(0).replaceAll("", ""));
      }

      String resp = getEchoBody("", headers, uri, request);

      return Response.status(status).header("Location", newLocation).entity(resp).build();
   }

   public Response getRawStatusCode(String statusCode, String location, HttpHeaders headers, UriInfo uri, HttpServletRequest request) throws URISyntaxException {

      int status;
      try {
         status = Integer.parseInt(statusCode);
      } catch (NumberFormatException e) {
         status = Response.Status.NOT_FOUND.getStatusCode();
      }

      List<String> emptyBody = headers.getRequestHeader("emptybody");

      String resp = "";
      if (emptyBody == null) {
         resp = getEchoBody("", headers, uri, request);
         return Response.status(status).entity(resp).build();
      }else{
         return Response.status(status).build();
      }

      
   }

   public Response postStatusCode(String body, String statusCode, String location, HttpHeaders headers, UriInfo uri, HttpServletRequest request) throws URISyntaxException {

      int status;
      try {
         status = Integer.parseInt(statusCode);
      } catch (NumberFormatException e) {
         status = Response.Status.NOT_FOUND.getStatusCode();
      }

      String resp = getEchoBody(body, headers, uri, request);

      if (status >= THREE_HUNDRED && status < FOUR_HUNDRED) {

         URI newLocation = new URI(location);

         return Response.status(status).header("Location", newLocation).entity(resp).build();
      }

      if (status == Response.Status.UNAUTHORIZED.getStatusCode()) {
         final com.rackspacecloud.docs.auth.api.v1.ObjectFactory of = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

         UnauthorizedFault fault = new UnauthorizedFault();
         fault.setCode(Response.Status.UNAUTHORIZED.getStatusCode());
         fault.setMessage("Beware!  You are unauthorized.");
         return Response.ok(of.createUnauthorized(fault)).status(Response.Status.UNAUTHORIZED).build();
      }

      if (status == Response.Status.NOT_FOUND.getStatusCode()) {
         final com.rackspacecloud.docs.auth.api.v1.ObjectFactory of = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

         UnauthorizedFault fault = new UnauthorizedFault();
         fault.setCode(Response.Status.NOT_FOUND.getStatusCode());
         fault.setMessage("Dude, I'd love to help you but I can't find what you're looking for.");
         return Response.ok(of.createUnauthorized(fault)).status(Response.Status.NOT_FOUND).build();
      }

      return Response.status(status).entity(resp).build();
   }

   public Response getDelayedResponse(int time, HttpHeaders headers, UriInfo uri, HttpServletRequest request) {
      int t = time;
      while (t > 0) {
         try {
            Thread.sleep(1);
            t--;
         } catch (InterruptedException e) {
         }

      }
      StringBuilder body = new StringBuilder("Response delayed by ");
      body.append(time).append(" milliseconds");
      return this.getEndService(body.toString(), headers, uri, request);
   }

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

   public Response getLBaaSLimitsXml() {

      StringBuilder limits = new StringBuilder();

      limits.append("<limits xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\">");
      limits.append("<absolute>");
      limits.append("<limit name=\"IPV6_LIMIT\" value=\"25\"/>");
      limits.append("<limit name=\"LOADBALANCER_LIMIT\" value=\"25\"/>");
      limits.append("<limit name=\"BATCH_DELETE_LIMIT\" value=\"10\"/>");
      limits.append("<limit name=\"ACCESS_LIST_LIMIT\" value=\"100\"/>");
      limits.append("<limit name=\"NODE_LIMIT\" value=\"25\"/>");
      limits.append("</absolute>");
      limits.append("</limits>");

      return Response.ok(limits.toString()).build();
   }

   public Response getRequestingIp(HttpServletRequest req) {

      return Response.ok(req.getRemoteAddr()).build();
   }
}
