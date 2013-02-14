package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA. User: joshualockwood Date: May 19, 2011 Time: 10:38:16 AM
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

        @Test
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

        @Test
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

        @Test
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
            actual = HeaderValuesImpl.fromMap(headers, "header1");

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnNullIfNotFound() {
            assertNull(HeaderValuesImpl.fromMap(headers, "headerZ"));
        }

        @Test
        public void shouldReturnNullHeadersCollectionIsEmpty() {
            assertNull(HeaderValuesImpl.fromMap(headers, "header2"));
        }
    }

    public static class WhenGettingPreferredHeaderValuesFromMap {

        private HttpServletRequest request;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private Enumeration<String> headerValues2;
        private MutableHttpServletRequest wrappedRequest;

        @Before
        public void setup() {

            headerNames = createStringEnumeration("header1", "HEADER2");

            headerValues1 = createStringEnumeration("val1.1;q=0.1", "val1.2;q=0.5", "val1.3;q=0.2", "val1.4;q=0.5");
            headerValues2 = createStringEnumeration("val2.1");

            request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(headerNames);
            when(request.getHeaders("header1")).thenReturn(headerValues1);
            when(request.getHeaders("header2")).thenReturn(headerValues2);

            wrappedRequest = MutableHttpServletRequest.wrap(request);

        }

        @Test
        public void shouldReturnFirstPreferredElementInMatchingHeader() {
            final HeaderValue expected = new HeaderValueImpl("val1.2", 0.5);
            final HeaderValue actual = wrappedRequest.getPreferredHeader("header1");

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnPreferredElementsInMatchingHeader() {
            final HeaderValue header1 = new HeaderValueImpl("val1.2", 0.5);
            final HeaderValue header2 = new HeaderValueImpl("val1.4", 0.5);
            final List<HeaderValue> expected = new ArrayList<HeaderValue>() {

                {
                    this.add(header1);
                    this.add(header2);
                }
            };
            final List<HeaderValue> actual = wrappedRequest.getPreferredHeaderValues("header1");

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnNullIfNotFound() {
            final HeaderValue actual = wrappedRequest.getPreferredHeader("headerZ");
            assertNull(actual);
        }

        @Test
        public void shouldReturnDefaultValueIfNotFound() {
            final HeaderValue expected = new HeaderValueImpl("default");
            final HeaderValue actual = wrappedRequest.getPreferredHeader("headerZ", expected);

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnDefaultValuesIfNotFound() {
            final HeaderValue defaultValue = new HeaderValueImpl("default", -1);
            final List<HeaderValue> expected = new ArrayList<HeaderValue>() {

                {
                    this.add(defaultValue);
                }
            };
            final List<HeaderValue> actual = wrappedRequest.getPreferredHeaderValues("headerZ", defaultValue);

            assertEquals(expected, actual);
        }
    }

    public static class WhenGettingPreferedOrderOfHeaders {

        private HttpServletRequest request;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private Enumeration<String> headerValues2;
        private Enumeration<String> headerValues3;
        private MutableHttpServletRequest wrappedRequest;

        @Before
        public void setup() {

            headerNames = createStringEnumeration("header1", "HEADER2", "header3");

            headerValues1 = createStringEnumeration("val1.1;q=1.0", "val1.2;q=0.5","val1.5;q=1.0", "val1.3;q=0.2", "val1.4;q=0.5");
            headerValues2 = createStringEnumeration("val2.1;q=0.8");
            headerValues3 = createStringEnumeration("val3.1;q=1.0");


            request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(headerNames);
            when(request.getHeaders("header1")).thenReturn(headerValues1);
            when(request.getHeaders("header2")).thenReturn(headerValues2);
            when(request.getHeaders("header3")).thenReturn(headerValues3);

            wrappedRequest = MutableHttpServletRequest.wrap(request);
        }

        @Test
        public void shouldReturnPreferedOrderListofHeaders() {
            final HeaderValue defaultValue = new HeaderValueImpl("default", -1);
            
            List<HeaderValue> list = wrappedRequest.getPreferredHeaders("header1", defaultValue);
            assertEquals("First Element should be first occurrence of the highest quality value",list.get(0).getValue(),"val1.1");
            assertEquals("Second Element should be the second occurrence of the highest quality value",list.get(1).getValue(), "val1.5");
            assertEquals(list.get(4).getValue(), "val1.3");
            
            
        }
    }
}
