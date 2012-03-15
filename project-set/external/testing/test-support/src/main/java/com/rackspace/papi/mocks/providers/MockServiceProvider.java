/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.mocks.providers;

import java.util.List;
import java.util.Set;
import javax.ws.rs.core.*;
import com.rackspace.papi.components.limits.schema.*;
import com.rackspace.papi.components.ratelimit.util.*;

public class MockServiceProvider {
    
    private ObjectFactory factory;
    private String[] absNames = {"Admin", "Tech", "Demo"};

    public MockServiceProvider() {
        factory = new ObjectFactory();
    }

    public Response getEndService(String body, HttpHeaders headers, UriInfo uri) {


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
        if (!body.isEmpty()){
            resp += "\n\t\t<h2>Body</h2>";
            resp += "\n\t\t\t<h3>"+body+"</h3>";
            
        }
        resp += "\n\t</body>\n</html>";
        return Response.ok(resp).header("x-request-id", "somevalue").build();
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
    
    public Response getStatusCode(String statusCode){
        
        int status;
        try{
            status = Integer.parseInt(statusCode);
        }catch (NumberFormatException e){
            status = 404;
        }
        
        return Response.status(status).build();
        
    }
}
