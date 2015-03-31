/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.versioning.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.junit.Assert.*;
import static org.openrepose.filters.versioning.testhelpers.ConfigurationDataCreator.createVersionIds;

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
