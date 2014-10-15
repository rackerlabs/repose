package org.openrepose.filters.ipidentity;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.filters.ipidentity.config.IpIdentityConfig;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IpIdentityHandlerFactoryTest {

    private static String DEFAULT_IP_VALUE = "10.0.0.1";
    private static Double QUALITY = 0.2;
    private HttpServletRequest request;
    private ReadableHttpServletResponse response;
    private IpIdentityHandler handler;
    private IpIdentityConfig config, config2;
    IpIdentityHandlerFactory factory;

    @Before
    public void setUp() {
        
        factory = new IpIdentityHandlerFactory();

        config = new IpIdentityConfig();
        config.setQuality(QUALITY);
        
        factory.configurationUpdated(config);

        handler = factory.buildHandler();
        request = mock(HttpServletRequest.class);
        response = mock(ReadableHttpServletResponse.class);

        when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
    }

    @Test
    public void shouldCatchConfigurationUpdate(){
        
        final Double QUALITY2 = 0.6;
        final String QUALITY2_VALUE = ";q=0.6";
        
        config2 = new IpIdentityConfig();
        config2.setQuality(QUALITY2);
        
        
        factory.configurationUpdated(config2);
        handler = factory.buildHandler();
        
        FilterDirector director = handler.handleRequest(request, response);
        assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString())).contains(DEFAULT_IP_VALUE + QUALITY2_VALUE));
        assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString())).contains(IpIdentityGroup.DEST_GROUP + QUALITY2_VALUE));
         
    }
    
    @Test
    public void shouldUseDefaultQuality(){
        
        final String DEFAULT_QUALITY_VALUE = ";q=0.1";
        
        config2 = new IpIdentityConfig();
        factory.configurationUpdated(config2);
        handler = factory.buildHandler();
        
        FilterDirector director = handler.handleRequest(request, response);
        assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString())).contains(DEFAULT_IP_VALUE + DEFAULT_QUALITY_VALUE));
        assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString())).contains(IpIdentityGroup.DEST_GROUP + DEFAULT_QUALITY_VALUE));
    }
    
    @Test
    public void shouldUseDefaultQualityIfConfigIsBlank(){
        
        final String DEFAULT_QUALITY_VALUE = ";q=0.1";
        
        config2 = new IpIdentityConfig();
        config2.setQuality(null);
        factory.configurationUpdated(config2);
        handler = factory.buildHandler();
        
        FilterDirector director = handler.handleRequest(request, response);
        assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString())).contains(DEFAULT_IP_VALUE + DEFAULT_QUALITY_VALUE));
        assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString())).contains(IpIdentityGroup.DEST_GROUP+DEFAULT_QUALITY_VALUE));
    }
}
