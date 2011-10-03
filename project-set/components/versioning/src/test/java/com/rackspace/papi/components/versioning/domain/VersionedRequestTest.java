package com.rackspace.papi.components.versioning.domain;

import java.util.List;
import org.junit.Before;
import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import java.util.LinkedList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class VersionedRequestTest {

    @Ignore
    public static class TestParent {

        protected List<MediaRange> mediaRangeList;
        protected ServiceVersionMapping mapping;

        @Before
        public void standUp() {
            mediaRangeList = new LinkedList<MediaRange>();
            mediaRangeList.add(new MediaRange(MediaType.UNKNOWN));

            mapping = new ServiceVersionMapping();
            mapping.setId("v1.0");
            mapping.setContextPath("_v1.0");
        }
    }
    
    public static class WhenIdentifyingUriFragments {
        @Test
        public void shouldIdentifyRootFragments() {
            assertEquals(0, VersionedRequest.indexOfUriFragment("/v1", "/v1"));
            assertEquals(0, VersionedRequest.indexOfUriFragment("/v1/", "/v1"));
        }
        
        @Test
        public void shouldIdentifyPrependedFragments() {
            assertEquals(0, VersionedRequest.indexOfUriFragment("/v1/requested/uri", "/v1"));
        }
        
        @Test
        public void shouldIdentifyEmbeddedFragments() {
            assertEquals(10, VersionedRequest.indexOfUriFragment("/versioned/v1/requested/uri", "/v1"));
        }
        
        @Test
        public void shouldIdentifyAppendedFragments() {
            assertEquals(24, VersionedRequest.indexOfUriFragment("/requested/uri/versioned/v1", "/v1"));
        }
        
        @Test
        public void shouldNotIdentifyPartiallyMatchingEmbeddedFragments() {
            assertEquals(-1, VersionedRequest.indexOfUriFragment("/versioned/v12/requested/uri", "/v1"));
        }
    }

    public static class WhenIdentifyingVersionsInRequestUris extends TestParent {
        @Test
        public void shouldIdentifyVersion() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/resource", "http://localhost/v1.0/resource");
            final VersionedRequest versionedRequest = new VersionedRequest(requestInfo, mapping, "http://localhost/");
            
            assertTrue(versionedRequest.requestBelongsToVersionMapping());
        }
        
        @Test
        public void shouldIdentifyVersionWithTrailingSlash() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/resource/", "http://localhost/v1.0/resource/");
            final VersionedRequest versionedRequest = new VersionedRequest(requestInfo, mapping, "http://localhost/");
            
            assertTrue(versionedRequest.requestBelongsToVersionMapping());
        }
        
        @Test
        public void shouldNotMatchPartialVersionMatches() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.01/resource/", "http://localhost/v1.01/resource/");
            final VersionedRequest versionedRequest = new VersionedRequest(requestInfo, mapping, "http://localhost/");
            
            assertFalse(versionedRequest.requestBelongsToVersionMapping());
        }
    }

    public static class WhenGeneratingInternalMappings extends TestParent {

        @Test(expected = IllegalArgumentException.class)
        public void shouldNotAcceptUriWithoutRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "a/requested/resource", "http://localhost/a/requested/resource");

            new VersionedRequest(requestInfo, mapping, "http://localhost/").asInternalURI();
        }

        @Test
        public void shouldHandleFuzzedRequests() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0a/requested/resource", "http://localhost/v1.0a/requested/resource");

            final String expected = "/_v1.0/v1.0a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/").asInternalURI());
        }

        @Test
        public void shouldHandleNonVersionedRequests() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/a/requested/resource", "http://localhost/a/requested/resource");

            final String expected = "/_v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/").asInternalURI());
        }

        @Test
        public void shouldHandleVersionedRequestsWithContextRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/context/v1.0/a/requested/resource", "http://localhost/context/v1.0/a/requested/resource");

            final String expected = "/context/_v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/context/").asInternalURI());
        }

        @Test
        public void shouldNotRewriteVersionedUri() {
            final String expected = "/_v1.0/a/requested/resource";
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, expected, "http://localhost/v1.0/a/requested/resource");

            final VersionedRequest request = new VersionedRequest(requestInfo, mapping, "http://localhost/context/");

            assertFalse(request.uriRequiresRewrite());
            assertEquals("Formatting internal URI must match " + expected, expected, request.asInternalURI());
        }

        @Test
        public void shouldHandleVersionedRequests() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/a/requested/resource", "http://localhost/v1.0/a/requested/resource");

            final String expected = "/_v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/").asInternalURI());
        }

        @Test
        public void shouldBuildAccurateURLs() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/a/requested/resource", "http://localhost/a/requested/resource");

            final String expected = "http://localhost/_v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/").asInternalURL());
        }
    }

    public static class WhenGeneratingExternalMappings extends TestParent {

        @Test
        public void shouldHandleExternalRequestsWithContextRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/a/requested/resource", "http://localhost/a/requested/resource");

            final String expected = "http://localhost/context/v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/context").asExternalURL());
        }
    }

    public static class WhenTestingExternalMappings extends TestParent {

        @Test
        public void shouldMatch() {
            final HttpRequestInfo versionOne = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/some/resource", "http://localhost/v1.0");
            final HttpRequestInfo versionOneWithResource = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/some/resource", "http://localhost/v1.0/some/resource");
            final HttpRequestInfo versionTwo = new HttpRequestInfoImpl(mediaRangeList, "/v2.0/some/resource", "http://localhost/v2.0/some/resource");

            assertTrue(new VersionedRequest(versionOne, mapping, "http://localhost").requestBelongsToVersionMapping());
            assertTrue(new VersionedRequest(versionOneWithResource, mapping, "http://localhost").requestBelongsToVersionMapping());
            assertFalse(new VersionedRequest(versionTwo, mapping, "http://localhost").requestBelongsToVersionMapping());
        }

        @Test
        public void shouldIdentifyOwningVersions() {
            final HttpRequestInfo versionOne = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/some/resource", "http://localhost/v1.0/some/resource");
            final HttpRequestInfo versionTwo = new HttpRequestInfoImpl(mediaRangeList, "/v2.0/some/resource", "http://localhost/v2.0/some/resource");

            assertTrue(new VersionedRequest(versionOne, mapping, "http://localhost").requestBelongsToVersionMapping());
            assertFalse(new VersionedRequest(versionTwo, mapping, "http://localhost").requestBelongsToVersionMapping());
        }
    }
}
