package org.openrepose.filters.versioning;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.systemmodel.*;
import org.openrepose.filters.versioning.config.MediaTypeList;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.config.ServiceVersionMappingList;
import org.openrepose.filters.versioning.domain.ConfigurationData;
import org.openrepose.filters.versioning.util.ContentTransformer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author malconis
 */
public class VersioningHandlerTest {
   VersioningHandler versioningHandler;
   ContentTransformer contentTransformer;
   ConfigurationData configurationData;
   Map<String, Destination> configuredHosts;
   Map<String, ServiceVersionMapping> configuredMappings;
   ServiceVersionMappingList mappings;
   ServiceVersionMapping version1, version2, version3, version4;
   ReposeCluster domain;
   Node localHost;
   DestinationEndpoint localEndpoint;
   HttpServletRequest request;

   @Before
   public void setUp() {
      domain = new ReposeCluster();
      domain.setFilters(mock(FilterList.class));

      localHost = new Node();
      localHost.setHostname("localhost");
      localHost.setHttpPort(8080);
      localHost.setId("localhost");
      localHost.setHttpPort(Integer.valueOf(8888));

      localEndpoint = new DestinationEndpoint();
      localEndpoint.setHostname("localhost");
      localEndpoint.setPort(8080);
      localEndpoint.setProtocol("http");
      localEndpoint.setId("localhost");

      version1 = new ServiceVersionMapping();
      version1.setId("/v1");
      version1.setPpDestId("localhost");
      MediaTypeList v1MediaTypeList = new MediaTypeList();
      org.openrepose.filters.versioning.config.MediaType v1MediaType1 = new org.openrepose.filters.versioning.config.MediaType();
      v1MediaType1.setBase("application/xml");
      v1MediaType1.setType("application/vnd.vendor.service-v1+xml");
      v1MediaTypeList.getMediaType().add(v1MediaType1);


      org.openrepose.filters.versioning.config.MediaType v1MediaType2 = new org.openrepose.filters.versioning.config.MediaType();
      v1MediaType2.setBase("application/xml");
      v1MediaType2.setType("application/vnd.rackspace; x=v1; y=json");
      v1MediaTypeList.getMediaType().add(v1MediaType2);

      version1.setMediaTypes(v1MediaTypeList);

      version2 = new ServiceVersionMapping();
      version2.setId("/v2");
      version2.setPpDestId("localhost");
      MediaTypeList v2MediaTypeList = new MediaTypeList();
      org.openrepose.filters.versioning.config.MediaType v2MediaType1 = new org.openrepose.filters.versioning.config.MediaType();
      v2MediaType1.setBase("application/xml");
      v2MediaType1.setType("application/vnd.vendor.service-v2+xml");
      v2MediaTypeList.getMediaType().add(v2MediaType1);



      version2.setMediaTypes(v2MediaTypeList);

      version3 = new ServiceVersionMapping();
      version3.setId("/v3");
      version3.setPpDestId("badHost");
      MediaTypeList v3MediaTypeList = new MediaTypeList();
      org.openrepose.filters.versioning.config.MediaType v3MediaType1 = new org.openrepose.filters.versioning.config.MediaType();
      v3MediaType1.setBase("application/xml");
      v3MediaType1.setType("application/vnd.vendor.service-v3+xml");
      v3MediaTypeList.getMediaType().add(v2MediaType1);
      version3.setMediaTypes(v3MediaTypeList);

//        version4 = new ServiceVersionMapping();
//        version4.setId("/v4");
//        version4.setPpHostId("localhost");
//        version4.setContextPath("/version4");
//        MediaTypeList v4MediaTypeList = new MediaTypeList();
//        org.openrepose.filters.versioning.config.MediaType v4MediaType1 = new org.openrepose.filters.versioning.config.MediaType();
//        v4MediaType1.setBase("application/xml");
//        v4MediaType1.setType("application/vnd.vendor.service-v2+xml");
//        v4MediaTypeList.getMediaType().add(v4MediaType1);
//        version4.setMediaTypes(v4MediaTypeList);

      mappings = new ServiceVersionMappingList();

      mappings.getVersionMapping().add(version1);
      mappings.getVersionMapping().add(version2);
      mappings.getVersionMapping().add(version3);


      configuredHosts = new HashMap<String, Destination>();
      configuredMappings = new HashMap<String, ServiceVersionMapping>();

      configuredHosts.put(localHost.getId(), localEndpoint);
      configuredMappings.put(version1.getId(), version1);
      configuredMappings.put(version2.getId(), version2);
      configuredMappings.put(version3.getId(), version3);

      configurationData = new ConfigurationData(domain, localHost, configuredHosts, configuredMappings);

      contentTransformer = mock(ContentTransformer.class);
      versioningHandler = new VersioningHandler(configurationData, contentTransformer, null);
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
      assertEquals("Response should be OK", director.getResponseStatusCode(), HttpServletResponse.SC_OK);

   }

   @Test
   public void shouldReturnOnRequestForVersionRoot() {
      when(request.getRequestURI()).thenReturn("/v1");
      when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
      when(request.getHeader("accept")).thenReturn("application/xml");
      FilterDirector director = new FilterDirectorImpl();
      director = versioningHandler.handleRequest(request, null);
      assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
       assertEquals("Response should be OK", director.getResponseStatusCode(), HttpServletResponse.SC_OK);
   }

   @Test
   public void shouldReturnMultipleChoices() {

      when(request.getRequestURI()).thenReturn("/nothingwerecognize");
      when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
      when(request.getHeader("accept")).thenReturn("application/xml");
      FilterDirector director = new FilterDirectorImpl();
      director = versioningHandler.handleRequest(request, null);
      assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
      assertEquals("Response should be Multiple Choises", director.getResponseStatusCode(), HttpServletResponse.SC_MULTIPLE_CHOICES);
   }

   @Test
   public void shouldPassRequest() {
      when(request.getRequestURI()).thenReturn("/v1/somethingelse");
      when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
      when(request.getHeader("accept")).thenReturn("application/xml");
      FilterDirector director = new FilterDirectorImpl();
      director = versioningHandler.handleRequest(request, null);
      assertTrue("Filter director should be set to send the request through", director.getFilterAction().equals(FilterAction.PASS));
      assertEquals("Response should still be set to Internal Server Error", director.getResponseStatusCode(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      // Versioning no longer sets URL.  This is determined by the filter chain
      //assertTrue("The request URL has been rewritten to include the mapped version", director.getRequestUrl().toString().endsWith("/version1/somethingelse"));
   }

   @Test
   public void shouldCatchBadMappingToHost() {
      when(request.getRequestURI()).thenReturn("/v3/somethingelse");
      when(request.getRequestURL()).thenReturn(new StringBuffer("http://repose.node.n01:9999/"));
      when(request.getHeader("accept")).thenReturn("application/xml");
      FilterDirector director = new FilterDirectorImpl();
      director = versioningHandler.handleRequest(request, null);
      assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
      assertEquals("Response should still be set to Bad Gateway", director.getResponseStatusCode(), HttpServletResponse.SC_BAD_GATEWAY);
   }

   @Test
   public void shouldCatchMalformedURL() {
      when(request.getRequestURI()).thenReturn("/v3/somethingelse");
      when(request.getRequestURL()).thenReturn(new StringBuffer("/repose.node.n01:9999/"));
      when(request.getHeader("accept")).thenReturn("application/xml");
      FilterDirector director = new FilterDirectorImpl();
      director = versioningHandler.handleRequest(request, null);
      assertTrue("Filter director should be set to send the request back", director.getFilterAction().equals(FilterAction.RETURN));
      assertEquals("Response should still be set to Bad Gateway", director.getResponseStatusCode(), HttpServletResponse.SC_BAD_GATEWAY);
   }

   @Test
   public void shouldSetAcceptFromMediaTypeParameter() {
      final String acceptHeader = "application/vnd.rackspace; x=v1; y=json";
      when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singleton("Accept")));
      when(request.getRequestURI()).thenReturn("/somethingthere");
      when(request.getRequestURL()).thenReturn(new StringBuffer("repose.node.n01:9999/"));
      when(request.getHeader("accept")).thenReturn(acceptHeader);
      when(request.getHeader("Accept")).thenReturn(acceptHeader);
      when(request.getHeaders("accept")).thenReturn(Collections.enumeration(Collections.singleton(acceptHeader)));
      when(request.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton(acceptHeader)));
      FilterDirector director = new FilterDirectorImpl();
      director = versioningHandler.handleRequest(request, null);
      assertTrue(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(CommonHttpHeader.ACCEPT.toString())).contains("application/xml"));
   }
}
