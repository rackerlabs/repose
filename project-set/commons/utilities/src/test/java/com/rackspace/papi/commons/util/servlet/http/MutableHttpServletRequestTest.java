package com.rackspace.papi.commons.util.servlet.http;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.Ignore;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 19, 2011
 * Time: 10:38:16 AM
 */
@RunWith(Enclosed.class)
public class MutableHttpServletRequestTest {
    private static Enumeration<String> createStringEnumeration(String... names) {
        Vector<String> namesCollection = new Vector<String>(names.length);

        namesCollection.addAll(Arrays.asList(names));

        return namesCollection.elements();
    }

    public static class WhenCreatingNewInstances {
        private HttpServletRequest originalRequest;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private Enumeration<String> headerValues2;
        private MutableHttpServletRequest wrappedRequest;

        @Before
        public void setup() {
            headerNames = createStringEnumeration("header1", "HEADER2");

            headerValues1 = createStringEnumeration("val1.1", "val1.2");
            headerValues2 = createStringEnumeration("val2.1");

            originalRequest = mock(HttpServletRequest.class);
            
            when(originalRequest.getHeaderNames()).thenReturn(headerNames);
            when(originalRequest.getHeaders("header1")).thenReturn(headerValues1);
            when(originalRequest.getHeaders("header2")).thenReturn(headerValues2);

            wrappedRequest = MutableHttpServletRequest.wrap(originalRequest);
        }

        @Test @Ignore
        public void shouldMapExpectedNumberOfHeaders() {
            Integer expected, actual = 0;
            expected = 2;

            Enumeration<String> headerNames = wrappedRequest.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                actual++;
                headerNames.nextElement();
            }

            assertEquals(expected, actual);
        }

        @Test @Ignore
        public void shouldMapHeaderNamesAsLowerCase() {
            Integer expected, actual = 0;
            expected = 1;

            Enumeration<String> headerNames = wrappedRequest.getHeaders("header2");

            while (headerNames.hasMoreElements()) {
                actual++;
                headerNames.nextElement();
            }

            assertEquals(expected, actual);
        }

        @Test @Ignore
        public void shouldMapHeaderNamesAndValues() {
            assertEquals("val1.1", wrappedRequest.getHeader("header1"));
        }

        @Test
        public void shouldCreateNewInstanceIfIsNotWrapperInstance() {
            HttpServletRequest original = originalRequest;
            MutableHttpServletRequest actual;

            actual = MutableHttpServletRequest.wrap(original);

            assertNotSame(original, actual);
        }
    }

    public static class WhenGettingHeaderValuesFromMap {
        private List<String> headerValues;
        private Map<String, List<String>> headers;

        @Before
        public void setup() {
            headerValues = new ArrayList<String>();
            headerValues.add("val1");
            headerValues.add("val2");
            headerValues.add("val3");

            headers = new HashMap<String, List<String>>();
            headers.put("header1", headerValues);
            headers.put("header2", new ArrayList<String>());
        }

        @Test
        public void shouldReturnFirstElementInMatchingHeader() {
            String expected, actual;

            expected = headerValues.get(0);
            actual = MutableHttpServletRequest.fromMap(headers, "header1");

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnNullIfNotFound() {
            assertNull(MutableHttpServletRequest.fromMap(headers, "headerZ"));
        }

        @Test
        public void shouldReturnNullHeadersCollectionIsEmpty() {
            assertNull(MutableHttpServletRequest.fromMap(headers, "header2"));
        }
    }
}
