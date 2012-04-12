package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.ip.config.WhiteList;
import com.rackspace.papi.filter.logic.FilterDirector;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class IpIdentityHandlerTest {

   private static String DEFAULT_IP_VALUE = "10.0.0.1";
   private static String WHITELIST_IP_VALUE = "10.0.0.1";
   private static String WL_QUALITY = "0.2";
   private static String WL_QUALITY_VALUE = ";q=0.2";
   private static String QUALITY = "0.2";
   private static String QUALITY_VALUE = ";q=0.2";
   private HttpServletRequest request;
   private ReadableHttpServletResponse response;
   private IpIdentityHandler handler;
   private IpIdentityConfig config;

   @Before
   public void setUp() {
      request = mock(HttpServletRequest.class);
      response = mock(ReadableHttpServletResponse.class);

      when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
   }

   /**
    * Test of handleRequest method, of class IpIdentityHandler.
    */
   @Test
   public void testHandleRequest() {
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      handler = new IpIdentityHandler(config);

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString()).contains(DEFAULT_IP_VALUE + QUALITY_VALUE));
      assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.GROUPS.toString()).contains(IpIdentityGroup.DEST_GROUP + QUALITY_VALUE));
   }

   @Test
   public void shouldAddWhiteListGroupAndQuality() {      
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      WhiteList whiteList = new WhiteList();
      whiteList.setQuality(WL_QUALITY);
      whiteList.getIpAddress().add(WHITELIST_IP_VALUE);
      config.setWhiteList(whiteList);
      handler = new IpIdentityHandler(config);

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString()).contains(DEFAULT_IP_VALUE + WL_QUALITY_VALUE));
      assertTrue("Should have IP_Super as a group", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.GROUPS.toString()).contains(IpIdentityGroup.DEFAULT_WHITELIST_GROUP + WL_QUALITY_VALUE));
   }
}
