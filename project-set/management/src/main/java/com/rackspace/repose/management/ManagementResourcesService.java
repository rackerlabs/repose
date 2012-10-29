package com.rackspace.repose.management;

import com.rackspace.repose.management.cache.ReposeCacheJMXClient;
import com.rackspace.repose.management.reporting.Report;
import com.rackspace.repose.management.reporting.ReposeReportMBeanAdapter;
import com.rackspace.repose.management.reporting.ReposeReportingJMXClient;

import javax.management.MalformedObjectNameException;

import javax.ws.rs.*;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 23, 2012
 * Time: 4:32:00 PM
 */
@Path("/management")
public class ManagementResourcesService {

    private final ReposeCacheJMXClient reposeCacheJMXClient;
    private final ReposeReportingJMXClient reposeReportingJMXClient;

    public ManagementResourcesService() throws IOException, MalformedObjectNameException {
        reposeCacheJMXClient = new ReposeCacheJMXClient();
        reposeReportingJMXClient = new ReposeReportingJMXClient();
    }

    @GET
    @Path("/reporting")
    @Produces({"application/json", "application/xml"})
    public Report getReportingData() {
        return new ReposeReportMBeanAdapter().getReportingData(reposeReportingJMXClient);        
    }

    @GET
    @Path("/datastore/token/{tenantId}/{token}")
    public String removeToken(@PathParam("tenantId") String tenantId, @PathParam("token") String token) {
        String response = "Successfully cleared cache.";

        if (!reposeCacheJMXClient.removeTokenAndRoles(tenantId, token)) {
            response = "Unable to clear token and roles cache for tenant " + tenantId;    
        }
        return response;
    }

    @GET
    @Path("/datastore/groups/{tenantId}/{token}")
    public String removeGroups(@PathParam("tenantId") String tenantId, @PathParam("token") String token) {
        String response = "Successfully cleared cache.";

        if (!reposeCacheJMXClient.removeTokenAndRoles(tenantId, token)) {
            response = "Unable to clear groups cache for tenant " + tenantId;
        }

        return response;
    }

    @GET
    @Path("/datastore/limits/{userId}")
    public String removeLimits(@PathParam("userId") String userId) {
        String response = "Successfully cleared cache.";

        if (!reposeCacheJMXClient.removeLimits(userId)) {
            response = "Unable to remove limits for user " + userId;
        }

        return response;
    }
}
