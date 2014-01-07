package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.components.versioning.config.MediaTypeList;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.util.http.HttpRequestInfo;
import com.rackspace.papi.components.versioning.util.http.UniformResourceInfo;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author malconis
 */
public class ConfigurationDataTest {

   ConfigurationData configurationData;
   Map<String, Destination> configuredHosts;
   Map<String, ServiceVersionMapping> configuredMappings;
   ServiceVersionMappingList mappings;
   ServiceVersionMapping version1, version2;
   Node localHost;
   DestinationEndpoint localEndpoint;
   ReposeCluster domain;

   @Before
   public void setUp() {
      domain = new ReposeCluster();
      domain.setFilters(mock(FilterList.class));

      localHost = new Node();
      localHost.setHostname("localhost");
      localHost.setHttpPort(8080);
      localHost.setHttpsPort(0);
      localHost.setId("localhost");

      localEndpoint = new DestinationEndpoint();
      localEndpoint.setHostname("localhost");
      localEndpoint.setPort(8080);
      localEndpoint.setProtocol("http");
      localEndpoint.setId("localhost");

      version1 = new ServiceVersionMapping();
      version1.setId("/v1");
      version1.setPpDestId("localhost");
      MediaTypeList v1MediaTypeList = new MediaTypeList();
      com.rackspace.papi.components.versioning.config.MediaType v1MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
      v1MediaType1.setBase("application/xml");
      v1MediaType1.setType("application/vnd.vendor.service-v1+xml");
      v1MediaTypeList.getMediaType().add(v1MediaType1);
      version1.setMediaTypes(v1MediaTypeList);

      version2 = new ServiceVersionMapping();
      version2.setId("/v2");
      version2.setPpDestId("localhost");
      MediaTypeList v2MediaTypeList = new MediaTypeList();
      com.rackspace.papi.components.versioning.config.MediaType v2MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
      v2MediaType1.setBase("application/xml");
      v2MediaType1.setType("application/vnd.vendor.service-v2+xml");
      v2MediaTypeList.getMediaType().add(v2MediaType1);
      version2.setMediaTypes(v1MediaTypeList);

      mappings = new ServiceVersionMappingList();

      mappings.getVersionMapping().add(version1);
      mappings.getVersionMapping().add(version2);

      configuredHosts = new HashMap<String, Destination>();
      configuredMappings = new HashMap<String, ServiceVersionMapping>();

      configuredHosts.put(localHost.getId(), localEndpoint);
      configuredMappings.put(version1.getId(), version1);
      configuredMappings.put(version2.getId(), version2);

      configurationData = new ConfigurationData(domain, localHost, configuredHosts, configuredMappings);

   }

   @Test
   public void shouldReturnHostForServiceMapping() throws VersionedHostNotFoundException {
      assertEquals(localEndpoint, configurationData.getHostForVersionMapping(version1));
   }

   @Test
   public void shouldReturnConfiguredHosts() {
      assertEquals(configuredHosts, configurationData.getConfiguredHosts());
   }

   @Test
   public void shouldReturnVersionedOriginServiceFromURI() throws VersionedHostNotFoundException {
      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      FilterDirector director = mock(FilterDirector.class);

      when(requestInfo.getUri()).thenReturn("/v1/service/rs");

      VersionedOriginService destination = configurationData.getOriginServiceForRequest(requestInfo, director);
      assertEquals("Should find proper host given a matched uri to a version mapping", localEndpoint, destination.getOriginServiceHost());
   }

   @Test
   public void shouldReturnVersionedOriginServiceFromAcceptHeader() throws VersionedHostNotFoundException {
      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      FilterDirector director = new FilterDirectorImpl();
      MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v1+xml", MimeType.APPLICATION_XML);
      when(requestInfo.getUri()).thenReturn("/service/rs");
      when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);

      VersionedOriginService destination = configurationData.getOriginServiceForRequest(requestInfo, director);
      assertEquals("Should find proper host given a matched uri to a version mapping", localEndpoint, destination.getOriginServiceHost());
   }

   @Test
   public void shouldReturnVersionChoicesAsList() {
      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v1+xml", MimeType.APPLICATION_XML, -1);
      when(requestInfo.getUri()).thenReturn("/v1");
      when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);

      VersionChoiceList versionChoiceList = new VersionChoiceList();
      versionChoiceList = configurationData.versionChoicesAsList(requestInfo);

      assertEquals("Should return a version choice list of the two configured versions", versionChoiceList.getVersion().size(), 2);

   }

   @Test
   public void shouldReturnIfRequestIsForVersions() {
      UniformResourceInfo uniformResourceInfo = mock(UniformResourceInfo.class);
      when(uniformResourceInfo.getUri()).thenReturn("/");

      assertTrue("Should return true that this request is for the service root", configurationData.isRequestForVersions(uniformResourceInfo));
   }

   @Test
   public void shouldReturnNullIfNoMatchForMediaRangeIsFound() throws VersionedHostNotFoundException {

      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      FilterDirector director = new FilterDirectorImpl();
      MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v3+xml", MimeType.APPLICATION_XML, -1);
      when(requestInfo.getUri()).thenReturn("/service/rs");
      when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);

      assertNull("Should find proper host given a matched uri to a version mapping", configurationData.getOriginServiceForRequest(requestInfo, director));
   }
}
