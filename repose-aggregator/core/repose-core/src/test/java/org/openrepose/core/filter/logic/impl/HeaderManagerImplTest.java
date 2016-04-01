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
package org.openrepose.core.filter.logic.impl;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class HeaderManagerImplTest {

    public static class WhenAppendingToHeader {

        private HeaderManagerImpl headerManagerImpl = new HeaderManagerImpl();
        private HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        @Test
        public void shouldAppendToHeadersAlreadyInSet() {
            final String key = "key";

            headerManagerImpl.putHeader(key, "1");
            headerManagerImpl.appendHeader(key, "2");

            final Iterator<String> valueIterator = headerManagerImpl.headersToAdd().get(HeaderName.wrap(key)).iterator();
            final String firstValue = valueIterator.next();

            if ("1".equals(firstValue)) {
                assertEquals("Should append header values already present in the value set.", "2", valueIterator.next());
            } else if ("2".equals(firstValue)) {
                assertEquals("Should append header values already present in the value set.", "1", valueIterator.next());
            } else {
                fail("Header manager should allow values to be appended to the same header key");
            }
        }

        @Test
        public void shouldAppendWhenHeaderAlreadyPresent() {
            when(mockRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn("127.0.0.0;q=.3");

            headerManagerImpl.appendToHeader(mockRequest, PowerApiHeader.USER.toString(), "username;q=1");

            Set<String> values = headerManagerImpl.headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));

            assertEquals("Should append header value if header already present.", "127.0.0.0;q=.3,username;q=1", values.iterator().next());
        }

        @Test
        public void shouldPutHeaderWhenHeaderNotPresent() {
            when(mockRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn(null);

            headerManagerImpl.appendToHeader(mockRequest, PowerApiHeader.USER.toString(), "username;q=1");

            Set<String> values = headerManagerImpl.headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));

            assertEquals("Should put header value if header not present.", "username;q=1", values.iterator().next());
        }

        @Test
        public void shouldPutQualityInHeader() {
            when(mockRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn(null);

            headerManagerImpl.putHeader(PowerApiHeader.USER.toString(), "username", 0.5);

            Set<String> values = headerManagerImpl.headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));

            assertEquals("Should put header value if header not present.", "username;q=0.5", values.iterator().next());
        }

        @Test
        public void shouldPutDefaultQualityInHeader() {
            when(mockRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn(null);

            headerManagerImpl.appendHeader(PowerApiHeader.USER.toString(), "username", 1.0);

            Set<String> values = headerManagerImpl.headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));

            assertEquals("Should put header value if header not present.", "username;q=1.0", values.iterator().next());
        }

               /*
            As per RFC 2616, section 4.2
            Multiple message-header fields with the same field-name MAY be present in a message if and only if the
            entire field-value for that header field is defined as a comma-separated list [i.e., #(values)].
            It MUST be possible to combine the multiple header fields into one "field-name: field-value" pair, without
            changing the semantics of the message, by appending each subsequent field-value to the first, each
            separated by a comma. The order in which header fields with the same field-name are received is
            therefore significant to the interpretation of the combined field value, and thus a proxy MUST NOT change
            the order of these field values when a message is forwarded.
         */

        @Test
        public void shouldPutHeadersInIncomingOrder() {

            headerManagerImpl = new HeaderManagerImpl();

            String[] requestHeaderValues = {"value1", "value2", "value3"};


            headerManagerImpl.appendHeader("header1", requestHeaderValues);

            Iterator<String> itr = headerManagerImpl.headersToAdd().get(HeaderName.wrap("header1")).iterator();

            int counter = 0;

            while (itr.hasNext()) {
                String str = itr.next();

                assertTrue("Header manager is set to add headers in the order in which they came", str.equals(requestHeaderValues[counter++]));
            }

        }
    }
}
