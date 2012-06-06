/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.mocks.providers;

import com.rackspace.papi.components.limits.schema.AbsoluteLimit;
import com.rackspace.papi.components.limits.schema.AbsoluteLimitList;
import com.rackspace.papi.components.limits.schema.Limits;
import com.rackspace.papi.components.limits.schema.ObjectFactory;
import com.rackspace.papi.components.ratelimit.util.LimitsEntityTransformer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response.ResponseBuilder;

public class MockServiceProvider {

    private ObjectFactory factory;
    private String[] absNames = {"Admin", "Tech", "Demo"};

    public MockServiceProvider() {
        factory = new ObjectFactory();
    }

    public Response getEndService(String body, HttpHeaders headers, UriInfo uri) {
        return getEndService(body, "200", headers, uri, new HashMap<String, String>());
    }
    
    public Response getEndService(String body, String statusCode, HttpHeaders headers, UriInfo uri, Map<String, String> responseHeaders) {
        int status;
        try {
            status = Integer.parseInt(statusCode);
        } catch (NumberFormatException e) {
            status = 404;
        }
        
        Set<String> headerPairs = headers.getRequestHeaders().keySet();
        Set<String> queryParams = uri.getQueryParameters().keySet();
        StringBuilder resp = new StringBuilder("<html>\n\t<head>\n\t\t<title>Servlet version</title>\n\t</head>\n\t<body>\n\t\t<h1>Servlet version at ");
        resp.append(uri.getPath()).append("</h1>");
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
        ResponseBuilder response = Response.status(status);
        
        for (String headerName: responseHeaders.keySet()) {
            response = response.header(headerName, responseHeaders.get(headerName));
        }
        
        resp.append("\n\t</body>\n</html>");
        return response.entity(resp.toString()).header("x-request-id", "somevalue").header("Content-Length", resp.length()).build();
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

    public Response getStatusCode(String statusCode) {

        int status;
        try {
            status = Integer.parseInt(statusCode);
        } catch (NumberFormatException e) {
            status = 404;
        }

        return Response.status(status).build();

    }

    public Response getStatusCode(String statusCode, String location, String body) throws URISyntaxException {

        int status;
        try {
            status = Integer.parseInt(statusCode);
        } catch (NumberFormatException e) {
            status = 404;
        }
        
        URI newLocation = new URI(location);

        return Response.status(status).contentLocation(newLocation).entity(body).build();

    }

    public Response getDelayedResponse(int time, HttpHeaders headers, UriInfo uri) {
        int t = time;
        while (t > 0) {
            try {
                Thread.sleep(1000);
                t--;
            } catch (InterruptedException e) {
            }

        }
        StringBuilder body = new StringBuilder("Response delayed by ");
        body.append(time).append(" seconds");
        return this.getEndService(body.toString(), headers, uri);
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
    
    public Response getRequestingIp(HttpServletRequest req){
        
        return Response.ok(req.getRemoteAddr()).build();
    }
}
