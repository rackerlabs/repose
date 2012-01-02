/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.domain.HostUtilities;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.net.MalformedURLException;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.commons.util.net.NetUtilities;


import com.rackspace.papi.model.FilterList;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
/**
 *
 * @author malconis
 */
public class RoutingTaggerTest {
    
    HttpServletRequest request;
    ReadableHttpServletResponse response;
    RoutingTagger routingTagger;
    PowerProxy systemModel;
    String myHostName, requestUri, nextHostName;

    @Before
    public void setUp() {
        
        requestUri = "/mock/request/uri";
        nextHostName = "nextHostToRoute";
        
        request = mock(HttpServletRequest.class);
        response = mock(ReadableHttpServletResponse.class);
        systemModel = new PowerProxy();
        
        when(request.getRequestURI()).thenReturn(requestUri);
        
        Host localHost = new Host();
        localHost.setHostname(NetUtilities.getLocalHostName());
        localHost.setFilters(mock(FilterList.class));
        systemModel.getHost().add(localHost);
        
        
        
    }
    
 
    /**
     * Test of handleRequest method, of class RoutingTagger.
     */
    @Test
    public void shouldNotChangeNextRouteWhenValueIsPresent() {
        routingTagger = new RoutingTagger(systemModel);
        when(request.getHeader(PowerApiHeader.NEXT_ROUTE.getHeaderKey())).thenReturn("http://mockendservice.com:8082");
        FilterDirector result = routingTagger.handleRequest(request, response);
        assertTrue("Should not change route destination",request.getHeader(PowerApiHeader.NEXT_ROUTE.getHeaderKey()).equals("http://mockendservice.com:8082"));
        
    }
    
    @Test
    public void shouldRouteToNextNonLocalHost() throws MalformedURLException{
        
        final Host nextHost = new Host();
        nextHost.setHostname(nextHostName);
        nextHost.setFilters(mock(FilterList.class));
        systemModel.getHost().add(nextHost);
        routingTagger = new RoutingTagger(systemModel);
        FilterDirector result = routingTagger.handleRequest(request, response);
        final String nextRoute = HostUtilities.asUrl(nextHost, requestUri);
        assertTrue("Should route to next non-localhost host",result.requestHeaderManager().headersToAdd().get(PowerApiHeader.NEXT_ROUTE.getHeaderKey().toLowerCase()).contains(nextRoute));
        
    }

}
