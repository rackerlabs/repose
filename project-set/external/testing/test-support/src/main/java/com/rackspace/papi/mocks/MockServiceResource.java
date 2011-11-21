package com.rackspace.papi.mocks;

import java.util.List;
import java.util.Set;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import com.rackspace.papi.components.limits.schema.*;

/**
 *
 * @author malconis
 */
@Path("/mockendservice/")
public class MockServiceResource {

    protected ObjectFactory factory;
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
                resp += "\n\t\t<h3> " + q + " : " + headers.getRequestHeader(q) + "</h3>";
            }
        }
        resp += "\n\t</body>\n</html>";
        return Response.ok(resp).build();
    }

    @GET
    @Path("/")
    public Response getService(@Context HttpHeaders headers, @Context UriInfo uri) {
        return this.getEndService(headers, uri);
    }

    @GET
    @Path("/limits")
    public Response getAbsoluteLimits() {

        Limits limits = new Limits();
        AbsoluteLimitList absList = buildAbsoluteLimits();
        limits.setAbsolute(absList);

        return Response.ok(factory.createLimits(limits)).build();
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
