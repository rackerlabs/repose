 package com.rackspace.papi.mocks;

import java.util.List;
import java.util.Set;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import com.rackspace.papi.components.limits.schema.*;
import com.rackspace.papi.components.ratelimit.util.*;

/**
 *
 * @author malconis
 */
@Path("/")
public class MockServiceResource {

    private ObjectFactory factory;
    private String[] absNames = {"Admin", "Tech", "Demo"};

    public MockServiceResource() {
        factory = new ObjectFactory();
    }

    @GET
    @Path("{id : .*}")
    public Response getEndService(@Context HttpHeaders headers, @Context UriInfo uri) {
        
        
        Set<String> headerPairs = headers.getRequestHeaders().keySet();
        Set<String> queryParams = uri.getQueryParameters().keySet();
        String resp = "<html>\n\t<head>\n\t\t<title>Servlet version</title>\n\t</head>\n\t<body>\n\t\t<h1>Servlet version at "
                      + uri.getPath() + "</h1>";
        List<String> header;
        if (!headerPairs.isEmpty()) {
            resp += "\n\t\t<h2>HEADERS</h2>";
            for (String h : headerPairs) {
                header = headers.getRequestHeader(h);
                for (String hh : header) {
                    resp += "\n\t\t<h3> " + h + " : " + hh + "</h3>";
                }
            }
        }
        if (!queryParams.isEmpty()) {
            resp += "\n\t\t<h2>Query Parameters</h2>";
            for (String q : queryParams) {
                resp += "\n\t\t<h3> " + q + " : " + uri.getQueryParameters().get(q) + "</h3>";
            }
        }
        resp += "\n\t</body>\n</html>";
        return Response.ok(resp).header("x-request-id", "somevalue").build();
    }

    @GET
    @Path("/")
    public Response getService(@Context HttpHeaders headers, @Context UriInfo uri) {
        return this.getEndService(headers, uri);
    }
    
    @GET
    @Path("/{service}/limits")
    @Produces("application/json")
    public Response getLimitsJson(){
        return getAbsoluteLimitsJson();
    }
    
    @GET
    @Path("/{service}/limits")
    @Produces("application/xml")
    public Response getLimitsXml(){
        return getAbsoluteLimits();
    }
    
    @GET
    @Path("/{version}/{user}/limits")
    @Produces("application/json")
    public Response getAbsoluteLimitsJson(){
        
        Limits limits = new Limits();
        AbsoluteLimitList absList = buildAbsoluteLimits();
        limits.setAbsolute(absList);
        
        LimitsEntityTransformer transformer = new LimitsEntityTransformer();
        return Response.ok(transformer.entityAsJson(limits)).build();
    }
    
    @GET
    @Path("/{version}/{user}/limits")
    @Produces("application/xml")
    public Response getAbsoluteLimits() {

        Limits limits = new Limits();
        AbsoluteLimitList absList = buildAbsoluteLimits();
        limits.setAbsolute(absList);

        return Response.ok(factory.createLimits(limits)).build();
    }
    
    @GET
    @Path("*/statuscode/{statusCode}")
    public Response getStatusCode(@PathParam("statusCode") String statusCode){
        
        int status;
        try{
            status = Integer.parseInt(statusCode);
        }catch (NumberFormatException e){
            status = 404;
        }
        
        return Response.status(status).build();
        
    }
    
    @GET
    @Path("/nova/limits")
    public Response getNovaLimits(){
        
        StringBuilder limits = new StringBuilder();
        
        limits.append("<limits xmlns:atom=\"http://www.w3.org/2005/Atom\"");
        limits.append(" xmlns=\"http://docs.openstack.org/common/api/v1.1\"><rates/><absolute><limit name=\"maxServerMeta\"");
        limits.append(" value=\"5\"/><limit name=\"maxPersonality\" value=\"5\"/><limit name=\"maxImageMeta\" value=\"5\"/><limit name=\"maxPersonalitySize\"");
        limits.append(" value=\"1000\"/><limit name=\"maxTotalInstances\" value=\"1000\"/><limit name=\"maxTotalRAMSize\" value=\"10240000\"/></absolute></limits>");
        
        return Response.ok(limits.toString()).build();
    }
   
    

    public AbsoluteLimitList buildAbsoluteLimits() {
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
}
