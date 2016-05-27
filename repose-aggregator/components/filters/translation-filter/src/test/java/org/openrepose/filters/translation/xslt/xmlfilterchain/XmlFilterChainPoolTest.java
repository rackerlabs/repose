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
package org.openrepose.filters.translation.xslt.xmlfilterchain;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.filters.translation.config.HttpMethod;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class XmlFilterChainPoolTest {

    public static class WhenMatchingPoolCriteria {

        private XmlChainPool responsePoolForXml;
        private XmlChainPool requestPoolForXml;
        private MediaType json = new MediaType(MimeType.getMatchingMimeType("application/json"));
        private MediaType xml = new MediaType(MimeType.getMatchingMimeType("application/xml"));

        @Before
        public void setUp() {
            List<HttpMethod> httpMethods = new ArrayList<HttpMethod>();
            httpMethods.add(HttpMethod.POST);
            responsePoolForXml = new XmlChainPool("application/xml", "application/xml", null, "4[\\d]{2}", "blah", null, null);
            requestPoolForXml = new XmlChainPool("application/xml", "application/xml", httpMethods, null, "blah", null, null);
        }

        @Test
        public void shouldAcceptResponseCriteria() {
            boolean actual = responsePoolForXml.accepts("", xml, xml, "400");
            assertTrue("Should accept our response values", actual);
        }

        @Test
        public void shouldRejectResponseCriteriaWhenContentTypeIsWrong() {
            boolean actual = responsePoolForXml.accepts("", json, xml, "400");
            assertFalse("Should reject invalid content type", actual);
        }

        @Test
        public void shouldRejectResponseCriteriaWhenAcceptTypeIsWrong() {
            boolean actual = responsePoolForXml.accepts("", xml, json, "400");
            assertFalse("Should reject invalid accept", actual);
        }

        @Test
        public void shouldRejectResponseCriteriaWhenResponseCodeWrong() {
            boolean actual = responsePoolForXml.accepts("", xml, xml, "200");
            assertFalse("Should reject invalid response code", actual);
        }

        @Test
        public void shouldAcceptRequestCriteria() {
            boolean actual = requestPoolForXml.accepts("POST", xml, xml, "");
            assertTrue("Should accept our response values", actual);
        }

        @Test
        public void shouldRejectRequestCriteriaWhenContentTypeIsWrong() {
            boolean actual = requestPoolForXml.accepts("", json, xml, "");
            assertFalse("Should reject invalid content type", actual);
        }

        @Test
        public void shouldRejectRequestCriteriaWhenAcceptTypeIsWrong() {
            boolean actual = requestPoolForXml.accepts("", xml, json, "");
            assertFalse("Should reject invalid accept", actual);
        }

        @Test
        public void shouldRejectRequestCriteriaWhenMethodWrong() {
            boolean actual = requestPoolForXml.accepts("GET", xml, xml, "");
            assertFalse("Should reject invalid method", actual);
        }
    }
}
