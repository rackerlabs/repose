/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import org.junit.Ignore;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.http.media.MediaType;
import java.util.HashMap;
import com.rackspace.papi.components.versioning.config.MediaTypeList;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import java.util.Map;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.model.Host;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author malconis
 */
public class VersioningHandlerTest {

    public VersioningHandlerTest() {
    }
    VersioningHandler versioningHandler;
    ContentTransformer contentTransformer;
    ConfigurationData configurationData;
    Map<String, Host> configuredHosts;
    Map<String, ServiceVersionMapping> configuredMappings;
    ServiceVersionMappingList mappings;
    ServiceVersionMapping version1, version2, version3, version4;
    Host localHost;
    HttpServletRequest request;

    @Before
    public void setUp() {

        localHost = new Host();
        localHost.setHostname("localhost");
        localHost.setFilters(mock(FilterList.class));
        localHost.setServicePort(8080);
        localHost.setId("localhost");
        localHost.setServicePort(Integer.valueOf(8888));

        version1 = new ServiceVersionMapping();
        version1.setId("/v1");
        version1.setPpHostId("localhost");
        version1.setContextPath("/version1");
        MediaTypeList v1MediaTypeList = new MediaTypeList();
        com.rackspace.papi.components.versioning.config.MediaType v1MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
        v1MediaType1.setBase("application/xml");
        v1MediaType1.setType("application/vnd.vendor.service-v1+xml");
        v1MediaTypeList.getMediaType().add(v1MediaType1);
        

        com.rackspace.papi.components.versioning.config.MediaType v1MediaType2 = new com.rackspace.papi.components.versioning.config.MediaType();
        v1MediaType2.setBase("application/xml");
        v1MediaType2.setType("application/vnd.rackspace; x=v1; y=json");
        v1MediaTypeList.getMediaType().add(v1MediaType2);
        
        version1.setMediaTypes(v1MediaTypeList);

        version2 = new ServiceVersionMapping();
        version2.setId("/v2");
        version2.setPpHostId("localhost");
        version2.setContextPath("/");
        MediaTypeList v2MediaTypeList = new MediaTypeList();        
        com.rackspace.papi.components.versioning.config.MediaType v2MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
        v2MediaType1.setBase("application/xml");
        v2MediaType1.setType("application/vnd.vendor.service-v2+xml");
        v2MediaTypeList.getMediaType().add(v2MediaType1);
        
        
        
        version2.setMediaTypes(v2MediaTypeList);
        
        version3 = new ServiceVersionMapping();
        version3.setId("/v3");
        version3.setPpHostId("badHost");
        version3.setContextPath("/");
        MediaTypeList v3MediaTypeList = new MediaTypeList();
        com.rackspace.papi.components.versioning.config.MediaType v3MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
        v3MediaType1.setBase("application/xml");
        v3MediaType1.setType("application/vnd.vendor.service-v3+xml");
        v3MediaTypeList.getMediaType().add(v2MediaType1);
        version3.setMediaTypes(v3MediaTypeList);
        
//        version4 = new ServiceVersionMapping();
//        version4.setId("/v4");
//        version4.setPpHostId("localhost");
//        version4.setContextPath("/version4");
//        MediaTypeList v4MediaTypeList = new MediaTypeList();
//        com.rackspace.papi.components.versioning.config.MediaType v4MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
//        v4MediaType1.setBase("application/xml");
//        v4MediaType1.setType("application/vnd.vendor.service-v2+xml");
//        v4MediaTypeList.getMediaType().add(v4MediaType1);
//        version4.setMediaTypes(v4MediaTypeList);

        mappings = new ServiceVersionMappingList();

        mappings.getVersionMapping().add(version1);
        mappings.getVersionMapping().add(version2);
        mappings.getVersionMapping().add(version3);


        configuredHosts = new HashMap<String, Host>();
        configuredMappings = new HashMap<String, ServiceVersionMapping>();

        configuredHosts.put(localHost.getId(), localHost);
        configuredMappings.put(version1.getId(), version1);
        configuredMappings.put(version2.getId(), version2);
        configuredMappings.put(version3.getId(), version3);

        configurationData = new ConfigurationData(localHost, configuredHosts, configuredMappings);

        contentTransformer = mock(ContentTransformer.class);
        versioningHandler = new VersioningHandler(configurationData, contentTransformer);
        request = mock(HttpServletRequest.class);
    }

    /**
     * Test of handleRequest method, of class VersioningHandler.
     */
    @Test
    public void shouldReturnOnRequestForServiceRoot() {

        when(request.getRequestURI()).thenReturn("/");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
        when(request.getHeader("accept")).thenReturn("application/xml");
        FilterDirector director = new FilterDirectorImpl();
        director = versioningHandler.handleRequest(request, null);
        assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
        assertTrue("Response should be OK", director.getResponseStatus().equals(HttpStatusCode.OK));

    }

    @Test
    public void shouldReturnOnRequestForVersionRoot() {
        when(request.getRequestURI()).thenReturn("/v1");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
        when(request.getHeader("accept")).thenReturn("application/xml");
        FilterDirector director = new FilterDirectorImpl();
        director = versioningHandler.handleRequest(request, null);
        assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
        assertTrue("Response should be OK", director.getResponseStatus().equals(HttpStatusCode.OK));
    }

    @Test
    public void shouldReturnMultipleChoices() {

        when(request.getRequestURI()).thenReturn("/nothingwerecognize");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
        when(request.getHeader("accept")).thenReturn("application/xml");
        FilterDirector director = new FilterDirectorImpl();
        director = versioningHandler.handleRequest(request, null);
        assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
        assertTrue("Response should be Multiple Choises", director.getResponseStatus().equals(HttpStatusCode.MULTIPLE_CHOICES));
    }

    @Test
    public void shouldPassRequest() {
        when(request.getRequestURI()).thenReturn("/v1/somethingelse");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
        when(request.getHeader("accept")).thenReturn("application/xml");
        FilterDirector director = new FilterDirectorImpl();
        director = versioningHandler.handleRequest(request, null);
        assertTrue("Filter director should be set to send the request through", director.getFilterAction().equals(FilterAction.PASS));
        assertTrue("Response should still be set to Internal Server Error", director.getResponseStatus().equals(HttpStatusCode.INTERNAL_SERVER_ERROR));
        assertTrue("The request URL has been rewritten to include the mapped version",director.getRequestUrl().toString().endsWith("/version1/somethingelse"));
    }
    
    @Test
    public void shouldCatchBadMappingToHost() {
        when(request.getRequestURI()).thenReturn("/v3/somethingelse");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
        when(request.getHeader("accept")).thenReturn("application/xml");
        FilterDirector director = new FilterDirectorImpl();
        director = versioningHandler.handleRequest(request, null);
        assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
        assertTrue("Response should still be set to Bad Gateway", director.getResponseStatus().equals(HttpStatusCode.BAD_GATEWAY));
    }
    
    @Test
    public void shouldCatchMalformedURL() {
        when(request.getRequestURI()).thenReturn("/v3/somethingelse");
        when(request.getRequestURL()).thenReturn(new StringBuffer("/repose.node.n01:9999/"));
        when(request.getHeader("accept")).thenReturn("application/xml");
        FilterDirector director = new FilterDirectorImpl();
        director = versioningHandler.handleRequest(request, null);
        assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
        assertTrue("Response should still be set to Bad Gateway", director.getResponseStatus().equals(HttpStatusCode.BAD_GATEWAY));
    }
    
    @Test
    public void shouldSetAcceptFromMediaTypeParameter() {
        when(request.getRequestURI()).thenReturn("/somethingthere");
        when(request.getRequestURL()).thenReturn(new StringBuffer("repose.node.n01:9999/"));
        when(request.getHeader("accept")).thenReturn("application/vnd.rackspace; x=v1; y=json");
        FilterDirector director = new FilterDirectorImpl();
        director = versioningHandler.handleRequest(request, null);
        assertTrue(director.requestHeaderManager().headersToAdd().get(CommonHttpHeader.ACCEPT.toString().toLowerCase()).contains("application/xml"));
    }
}
