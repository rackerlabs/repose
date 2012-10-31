package com.rackspace.repose.management;

import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import com.rackspace.repose.management.cache.ReposeCacheJMXClient;
import com.rackspace.repose.management.config.*;
import com.rackspace.repose.management.reporting.Report;
import com.rackspace.repose.management.reporting.ReposeReportMBeanAdapter;
import com.rackspace.repose.management.reporting.ReposeReportingJMXClient;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;

import javax.management.MalformedObjectNameException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 23, 2012
 * Time: 4:32:00 PM
 */
@Path("/management")
public class ManagementResourcesService {

    // TODO: Unhardcode this
    private static final String REPOSE_CONFIG_DIRECTORY = "/etc/repose/";

    private final ReposeCacheJMXClient reposeCacheJMXClient;
    private final ReposeReportingJMXClient reposeReportingJMXClient;

    public ManagementResourcesService() throws IOException, MalformedObjectNameException {
        reposeCacheJMXClient = new ReposeCacheJMXClient();
        reposeReportingJMXClient = new ReposeReportingJMXClient();
    }

    // REPORTING *********************************************************

    @GET
    @Path("/reporting")
    @Produces({"application/json", "application/xml"})
    public Report getReportingData() {
        return new ReposeReportMBeanAdapter().getReportingData(reposeReportingJMXClient);
    }

    // DATASTORE *********************************************************

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

    // CONFIGURATION *********************************************************

    @PUT
    @Path("/config/container")
    @Consumes("application/xml")
    public Response updateContainerConfiguration(ContainerConfiguration config) {

        try {
            new ContainerMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/container")
    @Produces("application/xml")
    public JAXBElement<ContainerConfiguration> getContainerConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<ContainerConfiguration> config = null;

        try {
            config = (JAXBElement<ContainerConfiguration>) new ContainerMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return config;
    }

    @PUT
    @Path("/config/system")
    @Consumes("application/xml")
    public Response updateSystemModelConfiguration(SystemModel config) {

        try {
            new SystemModelMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/system")
    @Produces("application/xml")
    public JAXBElement<SystemModel> getSystemModelConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<SystemModel> config = null;

        try {
            config = (JAXBElement<SystemModel>) new SystemModelMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return config;
    }

    @PUT
    @Path("/config/rms")
    @Consumes("application/xml")
    public Response updateResponseMessagingConfiguration(ResponseMessagingConfiguration config) {

        try {
            new ResponseMessagingMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/rms")
    @Produces("application/xml")
    public JAXBElement<ResponseMessagingConfiguration> getResponseMessagingConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<ResponseMessagingConfiguration> config = null;

        try {
            config = (JAXBElement<ResponseMessagingConfiguration>) new ResponseMessagingMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return config;
    }

    @PUT
    @Path("/config/ratelimiting")
    @Consumes("application/xml")
    public Response updateRateLimitingConfiguration(RateLimitingConfiguration config) {

        try {
            new RateLimitingMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/ratelimiting")
    @Produces("application/xml")
    public JAXBElement<RateLimitingConfiguration> getRateLimitingConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<RateLimitingConfiguration> config = null;

        try {
            config = (JAXBElement<RateLimitingConfiguration>) new RateLimitingMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return config;
    }

    @PUT
    @Path("/config/versioning")
    @Consumes("application/xml")
    public Response updateVersioningConfiguration(ServiceVersionMappingList config) {

        try {
            new VersioningMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/versioning")
    @Produces("application/xml")
    public JAXBElement<ServiceVersionMappingList> getVersioningConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<ServiceVersionMappingList> config = null;

        try {
            config = (JAXBElement<ServiceVersionMappingList>) new VersioningMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return config;
    }
}
