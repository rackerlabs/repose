package com.rackspace.papi.components.versioning.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Set;

import static com.rackspace.papi.components.versioning.testhelpers.ConfigurationDataCreator.createVersionIds;
import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/14/11
 * Time: 2:55 PM
 */
@RunWith(Enclosed.class)
public class RequestUrlTokenizerTest {
    public static class WhenTokenizingUrlsWithoutVersionInfo {
        private static final String REQUEST_URL = "http://rackspacecloud.com/resource/info";
        private RequestUrlTokenizer tokenizer;

        @Before
        public void setup() {
            tokenizer = RequestUrlTokenizer.tokenize(REQUEST_URL, createVersionIds(2));
        }

        @Test
        public void shouldExposeServiceRootHref() {
            assertEquals("http://rackspacecloud.com/", tokenizer.getServiceRootHref());
        }

        @Test
        public void shouldExposeVersionId() {
            assertEquals("", tokenizer.getVersionId());
        }

        @Test
        public void shouldExposeResourceInfo() {
            assertEquals("/resource/info", tokenizer.getResource());
        }
    }

    public static class WhenTokenizingUrlsWithoutVersionInfoOrResourceInfo {
        private static final String REQUEST_URL = "http://rackspacecloud.com/";
        private RequestUrlTokenizer tokenizer;

        @Before
        public void setup() {
            tokenizer = RequestUrlTokenizer.tokenize(REQUEST_URL, createVersionIds(2));
        }

        @Test
        public void shouldExposeServiceRootHref() {
            assertEquals("http://rackspacecloud.com/", tokenizer.getServiceRootHref());
        }

        @Test
        public void shouldExposeVersionId() {
            assertEquals("", tokenizer.getVersionId());
        }

        @Test
        public void shouldExposeResourceInfo() {
            assertEquals("", tokenizer.getResource());
        }
    }

    public static class WhenTokenizingUrlsWithVersionInfo {
        private static final String REQUEST_URL = "http://rackspacecloud.com/v1.1/resource/info";
        private RequestUrlTokenizer tokenizer;

        @Before
        public void setup() {
            tokenizer = RequestUrlTokenizer.tokenize(REQUEST_URL, createVersionIds(2));
        }

        @Test
        public void shouldExposeServiceRootHref() {
            assertEquals("http://rackspacecloud.com/", tokenizer.getServiceRootHref());
        }

        @Test
        public void shouldExposeVersionId() {
            assertEquals("v1.1", tokenizer.getVersionId());
        }

        @Test
        public void shouldExposeResourceInfo() {
            assertEquals("/resource/info", tokenizer.getResource());
        }
    }

    public static class WhenCheckingIfHasVersionInfo {
        private Set<String> versionIds;

        @Before
        public void setup() {
            versionIds = createVersionIds(2);
        }

        @Test
        public void shouldReturnTrueIfIsInVersionIdSet() {
            assertTrue(RequestUrlTokenizer.hasVersionInfo("v1.0", versionIds));
        }

        @Test
        public void shouldReturnFalseIfIsNotInVersionIdSet() {
            assertFalse(RequestUrlTokenizer.hasVersionInfo("version-1", versionIds));
        }
    }
}
