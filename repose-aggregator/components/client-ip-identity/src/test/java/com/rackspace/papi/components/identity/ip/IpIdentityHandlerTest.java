package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.net.IpAddressRange;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.components.identity.ip.config.WhiteList;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IpIdentityHandlerTest {

   private static String DEFAULT_IP_VALUE = "10.0.0.1";
   private static String WHITELIST_IP_VALUE = "10.0.0.1";
   private static Double WL_QUALITY = 0.2;
   private static String WL_QUALITY_VALUE = ";q=0.2";
   private static Double QUALITY = 0.2;
   private static String QUALITY_VALUE = ";q=0.2";
   private HttpServletRequest request;
   private ReadableHttpServletResponse response;
   private IpIdentityHandler handler;
   private IpIdentityConfig config;
   private IpIdentityHandlerFactory factory;

   @Before
   public void setUp() {
      request = mock(HttpServletRequest.class);
      response = mock(ReadableHttpServletResponse.class);
      factory = new IpIdentityHandlerFactory();

      when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
   }

   /**
    * Test of handleRequest method, of class IpIdentityHandler.
    */
   @Test
   public void testHandleRequest() {
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      factory.configurationUpdated(config);
      handler = factory.buildHandler();

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString()).contains(DEFAULT_IP_VALUE + QUALITY_VALUE));
      assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.GROUPS.toString()).contains(IpIdentityGroup.DEST_GROUP + QUALITY_VALUE));
   }
   
   private List<IpAddressRange> buildRanges(WhiteList list) {
      List<IpAddressRange> ranges = new ArrayList<IpAddressRange>();
      
      for (String address: list.getIpAddress()) {
         try {
            ranges.add(new IpAddressRange(address));
         } catch (UnknownHostException ex) {
         }
      }
      
      return ranges;
   }

   @Test
   public void shouldAddWhiteListGroupAndQuality() {      
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      WhiteList whiteList = new WhiteList();
      whiteList.setQuality(WL_QUALITY);
      whiteList.getIpAddress().add(WHITELIST_IP_VALUE);
      config.setWhiteList(whiteList);
      //handler = new IpIdentityHandler(config, buildRanges(whiteList));
      
      factory.configurationUpdated(config);
      handler = factory.buildHandler();

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString()).contains(DEFAULT_IP_VALUE + WL_QUALITY_VALUE));
      assertTrue("Should have IP_Super as a group", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.GROUPS.toString()).contains(IpIdentityGroup.DEFAULT_WHITELIST_GROUP + WL_QUALITY_VALUE));
   }

   @Test
   public void shouldUseXForwardedForHeaderWithWhitelistRange() {      
      final String IP = "192.168.1.1";
      final String NETWORK = "192.168.0.0/16";
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      WhiteList whiteList = new WhiteList();
      whiteList.setQuality(WL_QUALITY);
      whiteList.getIpAddress().add(NETWORK);
      config.setWhiteList(whiteList);
      factory.configurationUpdated(config);
      handler = factory.buildHandler();
      
      when(request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn(IP);

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString()).contains(IP + WL_QUALITY_VALUE));
      assertTrue("Should have IP_Super as a group", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.GROUPS.toString()).contains(IpIdentityGroup.DEFAULT_WHITELIST_GROUP + WL_QUALITY_VALUE));
   }

   @Test
   public void shouldUseXForwardedForHeader() {      
      final String IP = "192.169.1.1";
      final String NETWORK = "192.168.0.0/16";
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      WhiteList whiteList = new WhiteList();
      whiteList.setQuality(WL_QUALITY);
      whiteList.getIpAddress().add(NETWORK);
      config.setWhiteList(whiteList);
      factory.configurationUpdated(config);
      handler = factory.buildHandler();
      
      when(request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn(IP);

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString()).contains(IP + WL_QUALITY_VALUE));
      assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(PowerApiHeader.GROUPS.toString()).contains(IpIdentityGroup.DEST_GROUP + QUALITY_VALUE));
   }
}
