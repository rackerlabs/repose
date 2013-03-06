package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class EndpointLocationBuilderTest {

   public static class WhenBuildLocalEndpointLocations {

      private EndpointLocationBuilder instance;
      private Node localhost;
      private HttpServletRequest request;
      private DestinationEndpoint localFullySpecifiedDestination;
      private DestinationEndpoint localMinimallySpecifiedDestination;
      private DestinationEndpoint localSchemeButNoPortDestination;
      private DestinationEndpoint localNoHostNameSpecifiedDestination;

      @Before
      public void setUp() {
         request = mock(HttpServletRequest.class);
         when(request.getScheme()).thenReturn("http");
         when(request.getLocalPort()).thenReturn(8080);

         localhost = new Node();
         
         localhost.setHttpPort(8080);
         localhost.setHttpsPort(0);
         localhost.setHostname("myhost");
         localhost.setId("local");

         localFullySpecifiedDestination = new DestinationEndpoint();
         
         localFullySpecifiedDestination.setId("destId");
         localFullySpecifiedDestination.setHostname("localhost");
         localFullySpecifiedDestination.setPort(8080);
         localFullySpecifiedDestination.setProtocol("http");
         localFullySpecifiedDestination.setRootPath("/root");
         localFullySpecifiedDestination.setDefault(true);

         localMinimallySpecifiedDestination = new DestinationEndpoint();
         
         localMinimallySpecifiedDestination.setId("minimalDestId");
         localMinimallySpecifiedDestination.setRootPath("/minimal-root");
         localMinimallySpecifiedDestination.setDefault(true);

         localSchemeButNoPortDestination = new DestinationEndpoint();
         
         localSchemeButNoPortDestination.setId("destId");
         localSchemeButNoPortDestination.setPort(0);
         localSchemeButNoPortDestination.setProtocol("http");
         localSchemeButNoPortDestination.setRootPath("/no-port-root");
         localSchemeButNoPortDestination.setDefault(true);

         localNoHostNameSpecifiedDestination = new DestinationEndpoint();
         
         localNoHostNameSpecifiedDestination.setId("destId");
         localNoHostNameSpecifiedDestination.setPort(8080);
         localNoHostNameSpecifiedDestination.setProtocol("http");
         localNoHostNameSpecifiedDestination.setRootPath("/root");
         localNoHostNameSpecifiedDestination.setDefault(true);
      }

      @Test
      public void shouldReturnLocalUriWhenNoHostNameSpecified() throws Exception {
         
         String uri = "/somepath";
         
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(localNoHostNameSpecifiedDestination, uri, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = "/root" + uri;
         final String expectedUrl = "http://localhost:8080" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUri, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }
      
      @Test
      public void shouldReturnLocalUriWhenNoProtocolHostPortSpecified() throws Exception {
         
         String uri = "/somepath";
         
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(localSchemeButNoPortDestination, uri, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = "/no-port-root" + uri;
         final String expectedUrl = "http://localhost:8080" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUri, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }
      
      @Test
      public void shouldReturnLocalUriWhenProtocolButNoPortSpecified() throws Exception {
         
         String uri = "/somepath";
         
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(localMinimallySpecifiedDestination, uri, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = "/minimal-root" + uri;
         final String expectedUrl = "http://localhost:8080" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUri, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }
      
      @Test
      public void shouldReturnLocalUriWhenHostPortMatchLocalhost() throws Exception {
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(localFullySpecifiedDestination, null, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = "/root";
         final String expectedUrl = "http://localhost:8080" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUri, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }
   }

   public static class WhenBuildRemoteEndpointLocations {

      private EndpointLocationBuilder instance;
      private Node localhost;
      private HttpServletRequest request;
      private DestinationEndpoint remoteFullySpecifiedDestination;
      private DestinationEndpoint localHostDifferentPortSpecifiedDestination;
      private DestinationEndpoint remoteNoRootPathSpecifiedDestination;

      @Before
      public void setUp() {
         request = mock(HttpServletRequest.class);
         when(request.getScheme()).thenReturn("http");
         when(request.getLocalPort()).thenReturn(8080);

         localhost = new Node();
         
         localhost.setHttpPort(8080);
         localhost.setHttpsPort(8443);
         localhost.setHostname("localhost");
         localhost.setId("local");

         remoteFullySpecifiedDestination = new DestinationEndpoint();
         
         remoteFullySpecifiedDestination.setId("destId");
         remoteFullySpecifiedDestination.setHostname("otherhost");
         remoteFullySpecifiedDestination.setPort(8080);
         remoteFullySpecifiedDestination.setProtocol("http");
         remoteFullySpecifiedDestination.setRootPath("/root");
         remoteFullySpecifiedDestination.setDefault(true);

         remoteNoRootPathSpecifiedDestination = new DestinationEndpoint();
         
         remoteNoRootPathSpecifiedDestination.setId("destId");
         remoteNoRootPathSpecifiedDestination.setHostname("otherhost");
         remoteNoRootPathSpecifiedDestination.setPort(8080);
         remoteNoRootPathSpecifiedDestination.setProtocol("http");
         remoteNoRootPathSpecifiedDestination.setDefault(true);

         localHostDifferentPortSpecifiedDestination = new DestinationEndpoint();
         
         localHostDifferentPortSpecifiedDestination.setId("destId");
         localHostDifferentPortSpecifiedDestination.setPort(8081);
         localHostDifferentPortSpecifiedDestination.setProtocol("http");
         localHostDifferentPortSpecifiedDestination.setRootPath("/root/");
         localHostDifferentPortSpecifiedDestination.setDefault(true);
      }

      @Test
      public void shouldReturnFullUrlInUriToStringForRemoteDispatch() throws Exception {
         
         String uri = "somepath";
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(remoteFullySpecifiedDestination, uri, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = "/root/" + uri;
         final String expectedUrl = "http://otherhost:8080" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUrl, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }

      @Test
      public void shouldReturnFullUrlInUriToStringForRemoteDispatch2() throws Exception {
         
         String uri = "/somepath";
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(remoteNoRootPathSpecifiedDestination, uri, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = uri;
         final String expectedUrl = "http://otherhost:8080" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUrl, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }

      @Test
      public void shouldReturnFullUrlInUriToStringForRemoteDispatch3() throws Exception {
         
         String uri = "somepath";
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(remoteNoRootPathSpecifiedDestination, uri, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = "/" + uri;
         final String expectedUrl = "http://otherhost:8080" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUrl, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }

      @Test
      public void shouldReturnFullUrlInUriToStringForLocalDifferentPortDispatch() throws Exception {
         
         String uri = "/somepath";
         
         instance = new EndpointLocationBuilder().init(localhost);

         DestinationLocation result = instance.build(localHostDifferentPortSpecifiedDestination, uri, request);
         
         assertNotNull(result);
         assertNotNull(result.getUri());
         assertNotNull(result.getUrl());
         
         final String expectedUri = "/root/" + uri;
         final String expectedUrl = "http://localhost:8081" + expectedUri;
         
         assertEquals(expectedUri, result.getUri().getPath());
         assertEquals(expectedUrl, result.getUri().toString());
         assertEquals(expectedUrl, result.getUrl().toExternalForm());
         
      }
   }
}
