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
package org.openrepose.filters.headeridentity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.filters.headeridentity.config.HeaderIdentityConfig;
import org.openrepose.filters.headeridentity.config.HttpHeader;
import org.openrepose.filters.headeridentity.config.HttpHeaderList;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class HeaderIdentityHandlerTest {

    public static class WhenHandlingRequests {

        private static String IP_HEADER_NAME_1 = "IP1";
        private static String IP_HEADER_NAME_2 = "IP2";
        private static String IP_HEADER_1 = "127.0.0.1";
        private static String IP_HEADER_2 = "127.0.0.2";
        private static String DEFAULT_IP_VALUE = "10.0.0.1";
        private static String QUALITY = "0.2";
        private static String QUALITY_VALUE = ";q=0.2";
        private static String DEFAULT_QUALITY_VALUE = ";q=0.1";
        private HeaderIdentityHandler handler;
        private HeaderIdentityHandlerFactory factory;
        private HttpServletRequest request;
        private ReadableHttpServletResponse response;
        private HeaderIdentityConfig config;

        @Before
        public void setUp() throws Exception {

            factory = new HeaderIdentityHandlerFactory();
            config = new HeaderIdentityConfig();
            HttpHeaderList headerList = new HttpHeaderList();

            // Tell the handler to look for two headers called IP1 and IP2
            //config.setQuality(QUALITY);

            HttpHeader header = new HttpHeader();
            header.setId(IP_HEADER_NAME_1);
            header.setQuality(new Double(QUALITY));
            headerList.getHeader().add(header);
            header = new HttpHeader();
            header.setId(IP_HEADER_NAME_2);
            headerList.getHeader().add(header);

            config.setSourceHeaders(headerList);
            factory.configurationUpdated(config);

            handler = factory.buildHandler();

            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

            when(request.getHeader(IP_HEADER_NAME_1)).thenReturn(IP_HEADER_1);
            when(request.getHeader(IP_HEADER_NAME_2)).thenReturn(IP_HEADER_2);
            when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
        }

        @Test
        public void shouldSetTheUserHeaderToTheHeaderIpValue() {
            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
            assertTrue("Should find IP address in header", values.contains(IP_HEADER_1 + QUALITY_VALUE));

            Set<String> groups = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString()));
            assertTrue("Should find Group name in header", groups.contains(IP_HEADER_NAME_1 + QUALITY_VALUE));
        }

        @Test
        public void shouldSetTheUserHeaderToThe2ndHeaderIpValue() {
            // Let's "erase" the first IP header value
            when(request.getHeader(IP_HEADER_NAME_1)).thenReturn(null);

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
            assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values.isEmpty());

            String ip = values.iterator().next();

            assertEquals("Should find 2nd IP address in header", IP_HEADER_2 + DEFAULT_QUALITY_VALUE, ip);
        }

        @Ignore
        public void shouldSetTheUserHeaderToTheDefaultIpValue() {
            config.getSourceHeaders().getHeader().clear();

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
            assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values.isEmpty());

            String ip = values.iterator().next();

            assertEquals("Should have the default IP address", DEFAULT_IP_VALUE + QUALITY_VALUE, ip);
        }
    }
}
