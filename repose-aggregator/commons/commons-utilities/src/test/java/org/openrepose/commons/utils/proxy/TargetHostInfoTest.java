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
package org.openrepose.commons.utils.proxy;

import org.junit.Test;

import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TargetHostInfoTest {
    private final String targetHost = "http://otherhost.com/mocks/test";
    private final String invalidTargetHost = "abcd?%;:,==some weird string";

    @Test
    public void getProxiedHostUri_returnsExpectedUri() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(targetHost);

        assertEquals("Returned URI was not the expected URI", new URI(targetHost), targetHostInfo.getProxiedHostUri());
    }

    @Test
    public void getProxiedHostUri_returnsNullOnInvalidUri() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(invalidTargetHost);

        assertNull("Returned URI was null as expected", targetHostInfo.getProxiedHostUri());
    }

    @Test
    public void getProxiedHostUrl_returnsExpectedUrl() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(targetHost);

        assertEquals("Returned URL was not the expected URL", new URL("http", "otherhost.com", -1, ""), targetHostInfo.getProxiedHostUrl());
    }

    @Test
    public void getProxiedHostUrl_returnsNullOnInvalidUrl() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(invalidTargetHost);

        assertNull("Returned URL was null as expected", targetHostInfo.getProxiedHostUrl());
    }
}
