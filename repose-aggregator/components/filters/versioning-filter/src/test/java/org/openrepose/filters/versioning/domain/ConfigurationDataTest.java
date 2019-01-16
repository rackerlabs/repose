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
package org.openrepose.filters.versioning.domain;

import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.systemmodel.config.*;
import org.openrepose.filters.versioning.config.MediaTypeList;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.config.ServiceVersionMappingList;
import org.openrepose.filters.versioning.schema.VersionChoiceList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author malconis
 */
public class ConfigurationDataTest {

    private ConfigurationData configurationData;
    private Map<String, Destination> configuredHosts;
    private ServiceVersionMapping version1;
    private Destination localEndpoint;

    @Before
    public void setUp() {
        Node localHost = new Node();
        localHost.setHostname("localhost");
        localHost.setHttpPort(8080);
        localHost.setHttpsPort(0);
        localHost.setId("localhost");

        localEndpoint = new Destination();
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
        version1.setMediaTypes(v1MediaTypeList);

        ServiceVersionMapping version2 = new ServiceVersionMapping();
        version2.setId("/v2");
        version2.setPpDestId("localhost");
        MediaTypeList v2MediaTypeList = new MediaTypeList();
        org.openrepose.filters.versioning.config.MediaType v2MediaType1 = new org.openrepose.filters.versioning.config.MediaType();
        v2MediaType1.setBase("application/xml");
        v2MediaType1.setType("application/vnd.vendor.service-v2+xml");
        v2MediaTypeList.getMediaType().add(v2MediaType1);
        version2.setMediaTypes(v1MediaTypeList);

        ServiceVersionMappingList mappings = new ServiceVersionMappingList();

        mappings.getVersionMapping().add(version1);
        mappings.getVersionMapping().add(version2);

        configuredHosts = new HashMap<>();
        Map<String, ServiceVersionMapping> configuredMappings = new HashMap<>();

        configuredHosts.put(localHost.getId(), localEndpoint);
        configuredMappings.put(version1.getId(), version1);
        configuredMappings.put(version2.getId(), version2);

        configurationData = new ConfigurationData(configuredHosts, configuredMappings);
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
        HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);

        when(request.getRequestURI()).thenReturn("/v1/service/rs");

        VersionedOriginService destination = configurationData.getOriginServiceForRequest(request);
        assertEquals("Should find proper host given a matched uri to a version mapping", localEndpoint, destination.getOriginServiceHost());
    }

    @Test
    public void shouldReturnVersionedOriginServiceFromAcceptHeader() throws VersionedHostNotFoundException {
        HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/service/rs");
        when(request.getPreferredSplittableHeadersWithParameters(HttpHeaders.ACCEPT)).thenReturn(Collections.singletonList("application/vnd.vendor.service-v1+xml"));

        VersionedOriginService destination = configurationData.getOriginServiceForRequest(request);
        assertEquals("Should find proper host given a matched uri to a version mapping", localEndpoint, destination.getOriginServiceHost());
    }

    @Test
    public void shouldReturnVersionChoicesAsList() {
        HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/v1");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/v1"));
        when(request.getPreferredSplittableHeadersWithParameters(HttpHeaders.ACCEPT)).thenReturn(Collections.singletonList("application/vnd.vendor.service-v1+xml"));

        VersionChoiceList versionChoiceList = configurationData.versionChoicesAsList(request);

        assertEquals("Should return a version choice list of the two configured versions", versionChoiceList.getVersion().size(), 2);
    }

    @Test
    public void shouldReturnIfRequestIsForVersions() {
        HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/");

        assertTrue("Should return true that this request is for the service root", configurationData.isRequestForVersions(request));
    }

    @Test
    public void shouldReturnNullIfNoMatchForMediaRangeIsFound() throws VersionedHostNotFoundException {
        HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/service/rs");

        assertNull("Should find proper host given a matched uri to a version mapping", configurationData.getOriginServiceForRequest(request));
    }
}
