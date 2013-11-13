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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import javax.ws.rs.core.MediaType;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 23, 2012
 * Time: 4:32:00 PM
 */

@Path("/management")
public class ManagementResourcesService {

    private static final Logger LOG = LoggerFactory.getLogger(ManagementResourcesService.class);

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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
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
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateContainerConfiguration(ContainerConfiguration config) {

        try {
            new ContainerMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            LOG.error("Problem marshalling container configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find container configuration", e);
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/container")
    @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<ContainerConfiguration> getContainerConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<ContainerConfiguration> config = null;

        try {
            config = (JAXBElement<ContainerConfiguration>) new ContainerMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            LOG.error("Problem unmarshalling container configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find container configuration", e);
        }

        return config;
    }

    @PUT
    @Path("/config/system")
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateSystemModelConfiguration(SystemModel config) {

        try {
            new SystemModelMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            LOG.error("Problem marshalling system configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find system configuration", e);
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/system")
    @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<SystemModel> getSystemModelConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<SystemModel> config = null;

        try {
            config = (JAXBElement<SystemModel>) new SystemModelMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            LOG.error("Problem unmarshalling system configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find system configuration", e);
        }

        return config;
    }

    @PUT
    @Path("/config/rms")
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateResponseMessagingConfiguration(ResponseMessagingConfiguration config) {

        try {
            new ResponseMessagingMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            LOG.error("Problem marshalling response messaging configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find response messaging configuration", e);
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/rms")
    @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<ResponseMessagingConfiguration> getResponseMessagingConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<ResponseMessagingConfiguration> config = null;

        try {
            config = (JAXBElement<ResponseMessagingConfiguration>) new ResponseMessagingMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            LOG.error("Problem unmarshalling response messaging configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find response messaging configuration", e);
        }

        return config;
    }

    @PUT
    @Path("/config/ratelimiting")
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateRateLimitingConfiguration(RateLimitingConfiguration config) {

        try {
            new RateLimitingMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            LOG.error("Problem marshalling rate limiting configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find rate limiting configuration", e);
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/ratelimiting")
    @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<RateLimitingConfiguration> getRateLimitingConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<RateLimitingConfiguration> config = null;

        try {
            config = new RateLimitingMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            LOG.error("Problem unmarshalling rate limiting configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find rate limiting configuration", e);
        }

        return config;
    }

    @PUT
    @Path("/config/versioning")
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateVersioningConfiguration(ServiceVersionMappingList config) {

        try {
            new VersioningMarshaller().marshal(REPOSE_CONFIG_DIRECTORY, config);
        } catch (JAXBException e) {
            LOG.error("Problem marshalling versioning configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find versioning configuration", e);
        }

        return Response.created(URI.create("")).build();
    }

    @GET
    @Path("/config/versioning")
    @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<ServiceVersionMappingList> getVersioningConfiguration() throws JAXBException, FileNotFoundException {

        JAXBElement<ServiceVersionMappingList> config = null;

        try {
            config = (JAXBElement<ServiceVersionMappingList>) new VersioningMarshaller().unmarshal(REPOSE_CONFIG_DIRECTORY);
        } catch (JAXBException e) {
            LOG.error("Problem unmarshalling versioning configuration", e);
        } catch (FileNotFoundException e) {
            LOG.error("Cannot find versioning configuration", e);
        }

        return config;
    }
}
