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

import java.util.*;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.openrepose.commons.test.RegexMatcher.matchesPattern;

/**
 * @author zinic
 */
public class HeaderValueImplTest {

    @Test
    public void shouldUseQualityFactor() {
        final HeaderValue first = new HeaderValueImpl("", 0.8), second = new HeaderValueImpl("", 0.2);

        assertEquals("Matching quality factors and values must return 0", 0, first.compareTo(first));
        assertEquals("Higher quality factors must return 1", 1, first.compareTo(second));
        assertEquals("Lesser quality factors must return -1", -1, second.compareTo(first));
    }

    @Test
    public void shouldHandleNullHeaderValues() {
        final HeaderValue first = new HeaderValueImpl("", 0.8);

        assertEquals("Null header values must compare against valid values as lesser than valid values", 1, first.compareTo(null));
    }

    @Test
    public void shouldHandleNullHeaderValueString() {
        final HeaderValue first = new HeaderValueImpl("", 0.8), second = new HeaderValueImpl(null, 0.8);

        assertEquals("Null header value strings must compare against valid header value strings as lesser than valid values", 1, first.compareTo(second));
        assertEquals("Null header value strings must compare against valid header value strings as lesser than valid values", -1, second.compareTo(first));
        assertEquals("Null header value strings must compare against null header values as equal", 0, second.compareTo(second));
    }

    @Test
    public void shouldCompareStringValuesWhenQualityFactorsAreEqual() {
        final HeaderValue first = new HeaderValueImpl("equal", 0.8), second = new HeaderValueImpl("equal", 0.8),
                third = new HeaderValueImpl("eqlam", 0.8);

        assertEquals("Comparing header values must match equal values", 0, first.compareTo(second));
        assertEquals("Comparing header values must return the String class compareTo value", 9, first.compareTo(third));
    }

    @Test
    public void shouldReturnEqualsWhenTwoHeadersAreTheSameEvenWithDifferingQualities() {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("param1", "1");
        parameters.put("param2", "2");
        parameters.put("param3", "3");

        final HeaderValueImpl headerValue = new HeaderValueImpl("value", parameters);

        parameters.put("q", "0.5");
        final HeaderValueImpl headerValue2 = new HeaderValueImpl("value", parameters);

        assertTrue("Two Header Values should return equivalent even with differing qualiteis", headerValue.equalsTo(headerValue2));

    }

    @Test(expected = MalformedHeaderValueException.class)
    public void shouldReturnThrowNumberFormatExceptionForUnparsableQualityFactors() {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("q", "nan");

        final HeaderValueImpl headerValue = new HeaderValueImpl("value", parameters);

        assertThat("Header value must match expected output", headerValue.getQualityFactor(), equalTo(-1));
    }

    @Test
    public void shouldIdentifyWhenHeaderValueHasNoQualityFactor() {
        final HeaderValueImpl headerValue = new HeaderValueImpl("value", Collections.<String, String>emptyMap());

        assertFalse("Header value correctly identify whether or not it has an assigned quality factor", headerValue.hasQualityFactor());
    }

    @Test
    public void shouldReturnNegativeOneWhenNoQualityFactorCanBeDetermined() {
        final HeaderValueImpl headerValue = new HeaderValueImpl("value", Collections.<String, String>emptyMap());

        assertThat("Header value must match expected output", headerValue.getQualityFactor(), equalTo(1.0));
    }

    @Test
    public void shouldOutputValueQualityFactor() {
        final HeaderValueImpl headerValue = new HeaderValueImpl("value", 0.5);

        assertEquals("Header value must match expected output", "value;q=0.5", headerValue.toString());
    }

    @Test
    public void shouldOutputValueParameters() {
        final List<Pattern> patterns = new ArrayList<>();
        for (int i : Arrays.asList(1, 2, 3)) {
            patterns.add(Pattern.compile(".*;?(param" + i + "=" + i + ");?.*"));
        }

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("param1", "1");
        parameters.put("param2", "2");
        parameters.put("param3", "3");

        final HeaderValueImpl headerValue = new HeaderValueImpl("value", parameters);
        for (Pattern expectedPattern : patterns) {
            assertThat(headerValue.toString(), matchesPattern(expectedPattern));
        }
    }

    @Test
    public void shouldOutputValueWithNoParameters() {
        final HeaderValueImpl headerValue = new HeaderValueImpl("value", Collections.<String, String>emptyMap());

        assertEquals("Header value should only contain value when no parameters are present.", "value", headerValue.toString());
    }

    @Test
    public void shouldOutPutEmptyWithNullValue() {
        final HeaderValueImpl headerValue = new HeaderValueImpl(null, Collections.<String, String>emptyMap());

        assertEquals("Header value should be blank when passed null.", "", headerValue.toString());
    }
}
