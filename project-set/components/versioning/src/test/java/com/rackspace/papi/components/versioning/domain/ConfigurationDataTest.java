/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.versioning.domain;

import java.util.HashMap;
import com.rackspace.papi.model.FilterList;
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
import com.rackspace.papi.model.Host;
import java.util.Collection;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author malconis
 */
public class ConfigurationDataTest {

    ConfigurationData configurationData;
    Map<String, Host> configuredHosts;
    Map<String, ServiceVersionMapping> configuredMappings;
    ServiceVersionMappingList mappings;
    ServiceVersionMapping version1, version2;
    Host localHost;

    @Before
    public void setUp() {
        localHost = new Host();
        localHost.setHostname("localhost");
        localHost.setFilters(mock(FilterList.class));
        localHost.setServicePort(8080);
        localHost.setId("localhost");
        
        version1 = new ServiceVersionMapping();
        version1.setId("/v1");
        version1.setPpHostId("localhost");
        version1.setContextPath("/version1");
        MediaTypeList v1MediaTypeList = new MediaTypeList();
        com.rackspace.papi.components.versioning.config.MediaType v1MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
        v1MediaType1.setBase("application/xml");
        v1MediaType1.setType("application/vnd.vendor.service-v1+xml");
        v1MediaTypeList.getMediaType().add(v1MediaType1);
        version1.setMediaTypes(v1MediaTypeList);
        
        version2 = new ServiceVersionMapping();
        version2.setId("/v2");
        version2.setPpHostId("localhost");
        version2.setContextPath("/");
        MediaTypeList v2MediaTypeList = new MediaTypeList();
        com.rackspace.papi.components.versioning.config.MediaType v2MediaType1 = new com.rackspace.papi.components.versioning.config.MediaType();
        v2MediaType1.setBase("application/xml");
        v2MediaType1.setType("application/vnd.vendor.service-v2+xml");
        v2MediaTypeList.getMediaType().add(v2MediaType1);
        version2.setMediaTypes(v1MediaTypeList);
        
        mappings = new ServiceVersionMappingList();
        
        mappings.getVersionMapping().add(version1);
        mappings.getVersionMapping().add(version2);

        configuredHosts = new HashMap<String, Host>();
        configuredMappings = new HashMap<String, ServiceVersionMapping>();
        
        configuredHosts.put(localHost.getId(), localHost);
        configuredMappings.put(version1.getId(), version1);
        configuredMappings.put(version2.getId(), version2);
        
        configurationData = new ConfigurationData(localHost, configuredHosts, configuredMappings);

    }
    
    @Test 
    public void shouldReturnHostForServiceMapping() throws VersionedHostNotFoundException{
        assertEquals(localHost, configurationData.getHostForVersionMapping(version1));
    }
    
    @Test
    public void shouldReturnConfiguredHosts(){
        assertEquals(configuredHosts, configurationData.getConfiguredHosts());
    }
    
    @Test
    public void shouldReturnVersionedOriginServiceFromURI() throws VersionedHostNotFoundException{
        HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
        FilterDirector director = mock(FilterDirector.class);
        
        when(requestInfo.getUri()).thenReturn("/v1/service/rs");
        
        VersionedOriginService destination = configurationData.getOriginServiceForRequest(requestInfo, director);
        assertEquals("Should find proper host given a matched uri to a version mapping",localHost, destination.getOriginServiceHost());
    }
    
    @Test
    public void shouldReturnVersionedOriginServiceFromAcceptHeader() throws VersionedHostNotFoundException{
        HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
        FilterDirector director = new FilterDirectorImpl();
        MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v1+xml", MimeType.APPLICATION_XML);
        when(requestInfo.getUri()).thenReturn("/service/rs");
        when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);
        
        VersionedOriginService destination = configurationData.getOriginServiceForRequest(requestInfo, director);
        assertEquals("Should find proper host given a matched uri to a version mapping",localHost, destination.getOriginServiceHost());
    }
    
    @Test
    public void shouldReturnVersionChoicesAsList(){
        HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
        MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v1+xml", MimeType.APPLICATION_XML);
        when(requestInfo.getUri()).thenReturn("/v1");
        when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);
        
        VersionChoiceList versionChoiceList = new VersionChoiceList();
        versionChoiceList = configurationData.versionChoicesAsList(requestInfo);
        
        assertEquals("Should return a version choice list of the two configured versions", versionChoiceList.getVersion().size(),2);
        
    }
    
    @Test
    public void shouldReturnIfRequestIsForVersions(){
        UniformResourceInfo uniformResourceInfo = mock(UniformResourceInfo.class);
        when(uniformResourceInfo.getUri()).thenReturn("/");
        
        assertTrue("Should return true that this request is for the service root", configurationData.isRequestForVersions(uniformResourceInfo));
    }
    
    @Test public void shouldReturnNullIfNoMatchForMediaRangeIsFound() throws VersionedHostNotFoundException{
        
        HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
        FilterDirector director = new FilterDirectorImpl();
        MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v3+xml", MimeType.APPLICATION_XML);
        when(requestInfo.getUri()).thenReturn("/service/rs");
        when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);
        
        assertNull("Should find proper host given a matched uri to a version mapping",configurationData.getOriginServiceForRequest(requestInfo, director));
    }
    
    
    
    
}
