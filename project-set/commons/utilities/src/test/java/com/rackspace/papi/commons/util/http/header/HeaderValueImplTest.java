package com.rackspace.papi.commons.util.http.header;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HeaderValueImplTest {

    public static class WhenComparingHeaderValues {

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
            final Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("param1", "1");
            parameters.put("param2", "2");
            parameters.put("param3", "3");
            
            final HeaderValueImpl headerValue = new HeaderValueImpl("value", parameters);
            
            parameters.put("q", "0.5");
            final HeaderValueImpl headerValue2 = new HeaderValueImpl("value", parameters);
            
            assertTrue("Two Header Values should return equivalent even with differing qualiteis", headerValue.equalsTo(headerValue2));
            
        }
    }

    public static class WhenGettingQualityFactor {

        @Test(expected = MalformedHeaderValueException.class)
        public void shouldReturnThrowNumberFormatExceptionForUnparsableQualityFactors() {
            final Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("q", "nan");

            final HeaderValueImpl headerValue = new HeaderValueImpl("value", parameters);

            assertTrue("Header value must match expected output", -1 == headerValue.getQualityFactor());
        }

        @Test
        public void shouldIdentifyWhenHeaderValueHasNoQualityFactor() {
            final HeaderValueImpl headerValue = new HeaderValueImpl("value", Collections.EMPTY_MAP);

            assertFalse("Header value correctly identify whether or not it has an assigned quality factor", headerValue.hasQualityFactor());
        }

        @Test
        public void shouldReturnNegativeOneWhenNoQualityFactorCanBeDetermined() {
            final HeaderValueImpl headerValue = new HeaderValueImpl("value", Collections.EMPTY_MAP);

            assertTrue("Header value must match expected output", 1 == headerValue.getQualityFactor());
        }
    }

    public static class WhenOutputtingHeaderValueAsString {

        @Test
        public void shouldOutputValueQualityFactor() {
            final HeaderValueImpl headerValue = new HeaderValueImpl("value", 0.5);

            assertEquals("Header value must match expected output", "value;q=0.5", headerValue.toString());
        }

        @Test
        public void shouldOutputValueParameters() {
            final Pattern expectedPattern = Pattern.compile("[^;]+;(param1=1;?)?(param2=2;?)?(param3=3;?)?");

            final Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("param1", "1");
            parameters.put("param2", "2");
            parameters.put("param3", "3");

            final HeaderValueImpl headerValue = new HeaderValueImpl("value", parameters);

            assertTrue("Header value: " + headerValue.toString() + " must match expected pattern", expectedPattern.matcher(headerValue.toString()).matches());
        }

        @Test
        public void shouldOutputValueWithNoParameters() {
            final HeaderValueImpl headerValue = new HeaderValueImpl("value", Collections.EMPTY_MAP);

            assertEquals("Header value should only contain value when no parameters are present.", "value", headerValue.toString());
        }
        
        @Test
        public void shouldOutPutEmptyWithNullValue() {
            final HeaderValueImpl headerValue = new HeaderValueImpl(null, Collections.EMPTY_MAP);

            assertEquals("Header value should be blank when passed null.", "", headerValue.toString());
        }
    }
}
