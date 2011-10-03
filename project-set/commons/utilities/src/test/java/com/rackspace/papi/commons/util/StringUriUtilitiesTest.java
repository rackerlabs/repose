package com.rackspace.papi.commons.util;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * 
 */
@RunWith(Enclosed.class)
public class StringUriUtilitiesTest {

    public static class WhenIdentifyingUriFragments {

        @Test
        public void shouldIdentifyRootFragments() {
            assertEquals(0, StringUriUtilities.indexOfUriFragment("/v1", "/v1"));
            assertEquals(0, StringUriUtilities.indexOfUriFragment("/v1/", "/v1"));
        }

        @Test
        public void shouldIdentifyPrependedFragments() {
            assertEquals(0, StringUriUtilities.indexOfUriFragment("/v1/requested/uri", "/v1"));
        }

        @Test
        public void shouldIdentifyEmbeddedFragments() {
            assertEquals(10, StringUriUtilities.indexOfUriFragment("/versioned/v1/requested/uri", "/v1"));
        }

        @Test
        public void shouldIdentifyAppendedFragments() {
            assertEquals(24, StringUriUtilities.indexOfUriFragment("/requested/uri/versioned/v1", "/v1"));
        }

        @Test
        public void shouldNotIdentifyPartiallyMatchingEmbeddedFragments() {
            assertEquals(-1, StringUriUtilities.indexOfUriFragment("/versioned/v12/requested/uri", "/v1"));
        }
    }

    public static class WhenFormattingURIs {

        @Test
        public void shouldAddRootReference() {
            assertEquals("Should add a root reference to a URI", "/a/resource", StringUriUtilities.formatUri("a/resource"));
        }

        @Test
        public void shouldRemoveTrailingSlash() {
            assertEquals("Should remove trailing slashes from a URI", "/a/resource", StringUriUtilities.formatUri("/a/resource/"));
        }
    }
}
