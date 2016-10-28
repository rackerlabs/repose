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
package org.openrepose.commons.utils.http.header;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HeaderFieldParserTest {

    public static class WhenParsingHeaderFields {

        private static final String BASIC_CONTENT_TYPE = "application/xml";
        private static final String SUPER_COMPLICATED_MEDIA_TYPE = "application/vnd.rackspace.services.a+xml; x=v1.0; q=.8, application/json; q=.5";

        @Test
        public void shouldHandleNullConstructorArguments() {
            final String nullString = null;
            final Enumeration<String> nullEnumeration = null;
            final Set<String> nullSet = null;

            assertTrue("", new HeaderFieldParser(nullString).parse().isEmpty());
            assertTrue("", new HeaderFieldParser(nullEnumeration).parse().isEmpty());
            assertTrue("", new HeaderFieldParser(nullSet).parse().isEmpty());
        }

        @Test
        public void shouldParseHeaderFields() {
            final List<HeaderValue> headerValues = new HeaderFieldParser(BASIC_CONTENT_TYPE).parse();

            assertTrue("Header field parser should find one field", headerValues.size() == 1);

            final HeaderValue actual = headerValues.get(0);

            assertEquals("Header value should match expected", BASIC_CONTENT_TYPE, actual.getValue());
            assertTrue("Header value should have no parameters", actual.getParameters().isEmpty());
        }

        @Test
        public void shouldParseMultiValueHeaderFields() {
            final List<String> rawHeaderValues = new LinkedList<>();
            rawHeaderValues.add("value one;q=0.1;x=v1.0");
            rawHeaderValues.add("value two;q=0.3");

            final List<HeaderValue> headerValues = new HeaderFieldParser(Collections.enumeration(rawHeaderValues)).parse();

            assertTrue("Header field parser should find two fields", headerValues.size() == 2);

            final HeaderValue first = headerValues.get(0);

            assertEquals("Header value should match expected", "value one", first.getValue());
            assertTrue("Header value should have two parameters", first.getParameters().size() == 2);
            assertEquals("Header value paramter 'x' should have value 'v1.0'", first.getParameters().get("x"), "v1.0");
            assertEquals("Header value quality factor whould be 0.1", (Double) 0.1, (Double) first.getQualityFactor());

            final HeaderValue second = headerValues.get(1);

            assertEquals("Header value should match expected", "value two", second.getValue());
            assertTrue("Header value should have one parameters", second.getParameters().size() == 1);
            assertEquals("Header value quality factor whould be 0.3", (Double) 0.3, (Double) second.getQualityFactor());
        }

        @Test
        public void shouldParseRawMultiValueHeaderFields() {
            final List<HeaderValue> headerValues = new HeaderFieldParser(SUPER_COMPLICATED_MEDIA_TYPE).parse();

            assertTrue("Header field parser should find two fields", headerValues.size() == 2);

            final HeaderValue first = headerValues.get(0);

            assertEquals("Header value should match expected", "application/vnd.rackspace.services.a+xml", first.getValue());
            assertTrue("Header value should have two parameters", first.getParameters().size() == 2);
            assertEquals("Header value paramter 'x' should have value 'v1.0'", first.getParameters().get("x"), "v1.0");
            assertEquals("Header value quality factor whould be 0.8", (Double) 0.8, (Double) first.getQualityFactor());

            final HeaderValue second = headerValues.get(1);

            assertEquals("Header value should match expected", "application/json", second.getValue());
            assertTrue("Header value should have one parameters", second.getParameters().size() == 1);
            assertEquals("Header value quality factor whould be 0.5", (Double) 0.5, (Double) second.getQualityFactor());
        }
    }
}
