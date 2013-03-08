package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.components.translation.config.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
