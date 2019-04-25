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
package org.openrepose.commons.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 *
 */
public class StringUriUtilitiesTest {

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

    @Test
    public void shouldAddRootReference() {
        assertEquals("Should add a root reference to a URI", "/a/resource", StringUriUtilities.formatUri("a/resource"));
    }

    @Test
    public void shouldRemoveTrailingSlash() {
        assertEquals("Should remove trailing slashes from a URI", "/a/resource", StringUriUtilities.formatUri("/a/resource/"));
    }

    @Test
    public void shouldRemovingExtraLeadingSlashes() {
        assertEquals("Should remove multiple leading slasshes from a URI", "/a/resource", StringUriUtilities.formatUri("//////////a/resource///"));
    }

    @Test
    public void shouldReturnRootContextURI() {
        assertEquals("Should not return an empty string when passed a root context URI", "/", StringUriUtilities.formatUri("/"));
    }

    @Test
    public void shouldReturnRootContextURI2() {
        assertEquals("Should not return an empty string when passed a root context URI", "/", StringUriUtilities.formatUri("/////////"));
    }

    @Test
    public void shouldReturnRootContextURI3() {
        assertEquals("Should not return an empty string when passed a root context URI", "/", StringUriUtilities.formatUri(""));
    }

    @Test
    public void shouldAddLeadingSlash() {
        String uri1 = "one/two";
        String uri2 = "three/four";
        String expected = "/" + uri1 + "/" + uri2;

        String actual = StringUriUtilities.concatUris(uri1, uri2);

        assertEquals(expected, actual);

    }

    @Test
    public void shouldNotRemoveExtraSlash() {
        String uri1 = "one/two/";
        String uri2 = "/three/four/";
        String expected = "/one/two//three/four/";

        String actual = StringUriUtilities.concatUris(uri1, uri2);

        assertEquals(expected, actual);

    }

    @Test
    public void shouldHandleOneString() {
        String uri1 = "one/two/";
        String expected = "/one/two/";

        String actual = StringUriUtilities.concatUris(uri1);

        assertEquals(expected, actual);

    }

    @Test
    public void shouldSkipEmptyStrings() {
        String uri1 = "one/two/";
        String uri2 = "/three/four/";
        String expected = "/one/two//three/four/";

        String actual = StringUriUtilities.concatUris("", "    ", uri1, " ", "", uri2, "");

        assertEquals(expected, actual);

    }

    @Test
    public void shouldHandleSingle() {
        String uri1 = "/";
        String uri2 = "/";
        String expected = "//";

        String actual = StringUriUtilities.concatUris(uri1, uri2);

        assertEquals(expected, actual);

    }

    @Test
    public void shouldNotChangeWhenEncodingNonEncodableCharacters() {
        String uri1 = "qwerasdfjklvcxhjkfe-3djfkdfs";

        String uri2 = StringUriUtilities.encodeUri(uri1);

        assertEquals(uri2, uri1);
    }

    @Test
    public void shouldChangeWhenEncodingEncodableCharacters() {
        String uri1 = "key$test";

        String uri2 = StringUriUtilities.encodeUri(uri1);

        assertEquals(uri2, "key%24test");
    }

}
