package com.rackspace.papi.components.versioning.domain;

import java.util.List;
import org.junit.Before;
import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import java.util.LinkedList;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class VersionedRequestMapperTest {

    public static class WhenFormattingURIs {

        @Test
        public void shouldAddRootReference() {
            assertEquals("Should add a root reference to a URI", "/a/resource", VersionedRequest.formatUri("a/resource"));
        }

        @Test
        public void shouldRemoveTrailingSlash() {
            assertEquals("Should remove trailing slashes from a URI", "/a/resource", VersionedRequest.formatUri("/a/resource/"));
        }
    }

    public static class WhenGeneratingInternalMappings {

        private List<MediaRange> mediaRangeList;
        private ServiceVersionMapping mapping;

        @Before
        public void standUp() {
            mediaRangeList = new LinkedList<MediaRange>();
            mediaRangeList.add(new MediaRange(MediaType.UNKNOWN));

            mapping = new ServiceVersionMapping();
            mapping.setId("_v1.0");
            mapping.setName("v1.0");
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldNotAcceptUriWithoutRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "a/requested/resource", "http://localhost/a/requested/resource");

            new VersionedRequest(requestInfo, mapping, "http://localhost/").asInternalURI();
        }

        @Test
        public void shouldHandleNonVersionedRequests() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/a/requested/resource", "http://localhost/a/requested/resource");

            final String expected = "/_v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/").asInternalURI());
        }

        @Test
        public void shouldHandleVersionedRequestsWithContextRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/context/v1.0/a/requested/resource", "http://localhost/v1.0/context/a/requested/resource");

            final String expected = "/context/_v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/context/").asInternalURI());
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

    public static class WhenGeneratingExternalMappings {

        private List<MediaRange> mediaRangeList;
        private ServiceVersionMapping mapping;

        @Before
        public void standUp() {
            mediaRangeList = new LinkedList<MediaRange>();
            mediaRangeList.add(new MediaRange(MediaType.UNKNOWN));

            mapping = new ServiceVersionMapping();
            mapping.setId("_v1.0");
            mapping.setName("v1.0");
        }

        @Test
        public void shouldHandleExternalRequestsWithContextRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/a/requested/resource", "http://localhost/a/requested/resource");

            final String expected = "http://localhost/context/v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping, "http://localhost/context").asExternalURL());
        }
    }

    public static class WhenTestingExternalMappings {

        private List<MediaRange> mediaRangeList;
        private ServiceVersionMapping mapping;

        @Before
        public void standUp() {
            mediaRangeList = new LinkedList<MediaRange>();
            mediaRangeList.add(new MediaRange(MediaType.UNKNOWN));

            mapping = new ServiceVersionMapping();
            mapping.setId("_v1.0");
            mapping.setName("v1.0");
        }

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
