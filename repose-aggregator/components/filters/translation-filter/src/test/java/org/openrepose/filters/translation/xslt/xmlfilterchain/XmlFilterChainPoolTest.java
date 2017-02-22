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
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.filters.translation.config.HttpMethod;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XmlFilterChainPoolTest {

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
        assertTrue("Should accept our response values", responsePoolForXml.accepts("", xml, xml, "400"));
    }

    @Test
    public void shouldRejectResponseCriteriaWhenContentTypeIsWrong() {
        assertFalse("Should reject invalid content type", responsePoolForXml.accepts("", json, xml, "400"));
    }

    @Test
    public void shouldRejectResponseCriteriaWhenAcceptTypeIsWrong() {
        assertFalse("Should reject invalid accept", responsePoolForXml.accepts("", xml, json, "400"));
    }

    @Test
    public void shouldRejectResponseCriteriaWhenResponseCodeWrong() {
        assertFalse("Should reject invalid response code", responsePoolForXml.accepts("", xml, xml, "200"));
    }

    @Test
    public void shouldAcceptRequestCriteria() {
        assertTrue("Should accept our response values", requestPoolForXml.accepts("POST", xml, xml, ""));
    }

    @Test
    public void shouldRejectRequestCriteriaWhenContentTypeIsWrong() {
        assertFalse("Should reject invalid content type", requestPoolForXml.accepts("", json, xml, ""));
    }

    @Test
    public void shouldRejectRequestCriteriaWhenAcceptTypeIsWrong() {
        assertFalse("Should reject invalid accept", requestPoolForXml.accepts("", xml, json, ""));
    }

    @Test
    public void shouldRejectRequestCriteriaWhenMethodWrong() {
        assertFalse("Should reject invalid method", requestPoolForXml.accepts("GET", xml, xml, ""));
    }
}
