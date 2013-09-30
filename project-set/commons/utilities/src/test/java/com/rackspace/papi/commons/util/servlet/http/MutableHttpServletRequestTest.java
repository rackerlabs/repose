package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.io.stream.ServletInputStreamWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
            headerNames = createStringEnumeration("accept", "ACCEPT-ENCODING");

            headerValues1 = createStringEnumeration("val1.1", "val1.2");
            headerValues2 = createStringEnumeration("val2.1");

            originalRequest = mock(HttpServletRequest.class);

            when(originalRequest.getHeaderNames()).thenReturn(headerNames);
            when(originalRequest.getHeaders("accept")).thenReturn(headerValues1);
            when(originalRequest.getHeaders("accept-encoding")).thenReturn(headerValues2);

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

            Enumeration<String> headerNames = wrappedRequest.getHeaders("accept-encoding");

            while (headerNames.hasMoreElements()) {
                actual++;
                headerNames.nextElement();
            }

            assertEquals(expected, actual);
        }

        @Test
        public void shouldMapHeaderNamesAndValues() {
            assertEquals("val1.1", wrappedRequest.getHeader("accept"));
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
            headers.put("accept", headerValues);
            headers.put("ACCEPT-ENCODING", new ArrayList<String>());
        }

        @Test
        public void shouldReturnFirstElementInMatchingHeader() {
            String expected, actual;

            expected = headerValues.get(0);
            actual = HeaderValuesImpl.fromMap(headers, "accept");

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnNullIfNotFound() {
            assertNull(HeaderValuesImpl.fromMap(headers, "headerZ"));
        }

        @Test
        public void shouldReturnNullHeadersCollectionIsEmpty() {
            assertNull(HeaderValuesImpl.fromMap(headers, "ACCEPT-ENCODING"));
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

            headerNames = createStringEnumeration("accept", "ACCEPT-ENCODING");

            headerValues1 = createStringEnumeration("val1.1;q=0.1", "val1.2;q=0.5", "val1.3;q=0.2", "val1.4;q=0.5");
            headerValues2 = createStringEnumeration("val2.1");

            request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(headerNames);
            when(request.getHeaders("accept")).thenReturn(headerValues1);
            when(request.getHeaders("ACCEPT-ENCODING")).thenReturn(headerValues2);

            wrappedRequest = MutableHttpServletRequest.wrap(request);

        }

        @Test
        public void shouldReturnFirstPreferredElementInMatchingHeader() {
            final HeaderValue expected = new HeaderValueImpl("val1.2", 0.5);
            final HeaderValue actual = wrappedRequest.getPreferredHeader("accept");

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
            final List<HeaderValue> actual = wrappedRequest.getPreferredHeaderValues("accept");

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

            headerNames = createStringEnumeration("accept", "ACCEPT-ENCODING", "header3");

            headerValues1 = createStringEnumeration("val1.1;q=1.0", "val1.2;q=0.5","val1.5;q=1.0", "val1.3;q=0.2", "val1.4;q=0.5");
            headerValues2 = createStringEnumeration("val2.1;q=0.8");
            headerValues3 = createStringEnumeration("val3.1;q=1.0");


            request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(headerNames);
            when(request.getHeaders("accept")).thenReturn(headerValues1);
            when(request.getHeaders("ACCEPT-ENCODING")).thenReturn(headerValues2);
            when(request.getHeaders("header3")).thenReturn(headerValues3);

            wrappedRequest = MutableHttpServletRequest.wrap(request);
        }

        @Test
        public void shouldReturnPreferedOrderListofHeaders() {
            final HeaderValue defaultValue = new HeaderValueImpl("default", -1);
            
            List<HeaderValue> list = wrappedRequest.getPreferredHeaders("accept", defaultValue);
            assertEquals("First Element should be first occurrence of the highest quality value",list.get(0).getValue(),"val1.1");
            assertEquals("Second Element should be the second occurrence of the highest quality value",list.get(1).getValue(), "val1.5");
            assertEquals(list.get(4).getValue(), "val1.3");
            
            
        }
    }

    public static class WhenGettingEntityLength{

        private HttpServletRequest request;
        private MutableHttpServletRequest wrappedRequest;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private String msg = "This is my test entity";

        @Before
        public void setup() throws IOException {
            request = mock(HttpServletRequest.class);

            headerNames = createStringEnumeration("content-length");
            headerValues1 = createStringEnumeration("2");

            when(request.getHeaders("content-length")).thenReturn(headerValues1);
            when(request.getHeader("content-length")).thenReturn("2");
        }

        @Test
        public void shouldReturnActualLengthOfEntity() throws IOException {

            when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(msg.getBytes())));
            wrappedRequest = MutableHttpServletRequest.wrap(request);

            final int realEntitySize = wrappedRequest.getRealBodyLength();

            assertEquals("Real entity length should reflect what is in the inputstream",realEntitySize,msg.length());
            assertFalse("Real entity length should not match content-length", String.valueOf(realEntitySize).equals(request.getHeader("content-length")));
        }

        @Test
        public void shouldNotAlterMessageWhenRetrievingActualEntityLength() throws IOException {

            when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(msg.getBytes())));
            wrappedRequest = MutableHttpServletRequest.wrap(request);
            final int realEntitySize = wrappedRequest.getRealBodyLength();
            ServletInputStream is = wrappedRequest.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            final byte[] internalBuffer = new byte[1024];

            long total = 0;
            int read;

            while ((read = is.read(internalBuffer)) != -1) {
                os.write(internalBuffer, 0, read);
                total += read;
            }

            String newMsg = new String(os.toByteArray());

            assertEquals("Retrieving size of message should not alter message",msg,newMsg);

        }

        @Test
        public void shouldReturn0OnEmptyBody() throws IOException {

            String empty = "";
            ServletInputStream in = new ServletInputStreamWrapper(new ByteArrayInputStream(empty.getBytes()));

            when(request.getInputStream()).thenReturn(in);
            wrappedRequest = MutableHttpServletRequest.wrap(request);

            final int realEntitySize = wrappedRequest.getRealBodyLength();
            assertTrue("Should return 0 on empty body", realEntitySize == 0);

        }
    }

    public static class WhenDealingWithNonSplittableHeaders{

        private HttpServletRequest request;
        private Enumeration<String> headerNames;
        private Enumeration<String> headerValues1;
        private Enumeration<String> headerValues2;
        private Enumeration<String> headerValues3;
        private MutableHttpServletRequest wrappedRequest;

        @Before
        public void setup() {

            headerNames = createStringEnumeration("header1", "header2", "header3");

            headerValues1 = createStringEnumeration("val1","val2","val3");
            headerValues2 = createStringEnumeration("val4");
            headerValues3 = createStringEnumeration("val5,val6,val7");


            request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(headerNames);
            when(request.getHeaders("header1")).thenReturn(headerValues1);
            when(request.getHeaders("header2")).thenReturn(headerValues2);
            when(request.getHeaders("header3")).thenReturn(headerValues3);

            wrappedRequest = MutableHttpServletRequest.wrap(request);
        }

        @Test
        public void shouldNotSplitHeaders(){

            Integer expected, actual = 0;
            expected = 1;

            Enumeration<String> headerNames = wrappedRequest.getHeaders("header3");

            while (headerNames.hasMoreElements()) {
                actual++;
                headerNames.nextElement();
            }

            assertEquals(actual,expected);

        }

    }
}
