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
package org.openrepose.filters.headeridmapping;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.filters.headeridmapping.config.HeaderIdMappingConfig;
import org.openrepose.filters.headeridmapping.config.HttpHeader;
import org.openrepose.filters.headeridmapping.config.HttpHeaderList;
import org.openrepose.core.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class HeaderIdMappingHandlerTest {

   public static class WhenHandlingRequests {
      private static String USER_HEADER_NAME_1 = "USER1";
      private static String GROUP_HEADER_NAME_1 = "GROUP1";
      private static String USER_HEADER_NAME_2 = "USER2";
      private static String GROUP_HEADER_NAME_2 = "GROUP2";
      private static String USER_HEADER_1 = "user1";
      private static String GROUP_HEADER_1 = "group1";
      private static String USER_HEADER_2 = "user2";
      private static String GROUP_HEADER_2 = "group2";
      private static Double QUALITY = Double.valueOf("0.2");
      private static String QUALITY_VALUE = ";q=0.2";
      private static String DEFAULT_QUALITY_VALUE = ";q=0.1";
      private HttpServletRequest request;
      private ReadableHttpServletResponse response;
      private HeaderIdMappingHandlerFactory factory;
      private HeaderIdMappingHandler handler;
      private HeaderIdMappingConfig config;

      @Before
      public void setUp() throws Exception {
         HttpHeaderList headerList = new HttpHeaderList();
         factory = new HeaderIdMappingHandlerFactory();

         // Tell the handler to look for two headers called IP1 and IP2
         config = new HeaderIdMappingConfig();
         //config.setQuality(QUALITY);
         
         HttpHeader header = new HttpHeader();
         header.setId(USER_HEADER_NAME_1);
         header.setUserHeader(USER_HEADER_NAME_1);
         header.setGroupHeader(GROUP_HEADER_NAME_1);
         header.setQuality(QUALITY);
         headerList.getHeader().add(header);
         
         header = new HttpHeader();
         header.setId(USER_HEADER_NAME_2);
         header.setUserHeader(USER_HEADER_NAME_2);
         header.setGroupHeader(GROUP_HEADER_NAME_2);
         headerList.getHeader().add(header);
         config.setSourceHeaders(headerList);
         factory.configurationUpdated(config);
         
         handler = factory.buildHandler();
         request = mock(HttpServletRequest.class);
         response = mock(ReadableHttpServletResponse.class);
         
         when(request.getHeader(USER_HEADER_NAME_1)).thenReturn(USER_HEADER_1);
         when(request.getHeader(GROUP_HEADER_NAME_1)).thenReturn(GROUP_HEADER_1);
         when(request.getHeader(USER_HEADER_NAME_2)).thenReturn(USER_HEADER_2);
         when(request.getHeader(GROUP_HEADER_NAME_2)).thenReturn(GROUP_HEADER_2);
      }

      @Test
      public void shouldSetTheUserHeaderToTheHeaderValue() {
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
         assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values.isEmpty());
         
         String userHeader = values.iterator().next();
         
         assertEquals("Should find value in header", USER_HEADER_1 + QUALITY_VALUE, userHeader);
         
         Set<String> groups = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString()));
         assertFalse("Should have " + PowerApiHeader.GROUPS.toString() + " header set.", groups.isEmpty());
         
         String group = groups.iterator().next();
         assertEquals("Should find Group name in header", GROUP_HEADER_1 + QUALITY_VALUE, group);
      }


      @Test
      public void shouldSetTheUserHeaderToThe2ndHeaderIpValue() {
         // Let's "erase" the first IP header value
         when(request.getHeader(USER_HEADER_NAME_1)).thenReturn(null);

         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
         assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values.isEmpty());
         
         String userHeader = values.iterator().next();
         
         assertEquals("Should find 2nd IP address in header", USER_HEADER_2 + DEFAULT_QUALITY_VALUE, userHeader);

         Set<String> groups = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString()));
         assertFalse("Should have " + PowerApiHeader.GROUPS.toString() + " header set.", groups.isEmpty());
         
         String group = groups.iterator().next();
         assertEquals("Should find Group name in header", GROUP_HEADER_2 + DEFAULT_QUALITY_VALUE, group);
      }

   }
}
