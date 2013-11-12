/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.header.translation;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.header.translation.config.*;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Iterator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author kush5342
 */
@RunWith(Enclosed.class)
public class HeaderTranslationHandlerTest {

    public static class WhenAddingRequestRemoveOriginalHeaders {

        private HeaderTranslationHandlerFactory factory;
        private HeaderTranslationHandler handler;
        private HttpServletRequest request;
        private HeaderTranslationType config;
        private Header header1;
        private String requestUri1 = "http://openrepose.org/service/servicection1";
        private String[] requestHeaders = {"X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"};
        private String[] requestHeaderValues = {"value1", "value2", "value3"};
        private String[] singleRequestHeaderValue = {"value1"};

        @Before
        public void setUp() {

            factory = new HeaderTranslationHandlerFactory();

            config = new HeaderTranslationType();


            header1 = new Header();
            header1.setOriginalName("X-User-Header");
            header1.getNewName().add("new-header1");
            header1.getNewName().add("new-header2");
            header1.setRemoveOriginal(Boolean.TRUE);

            config.getHeader().add(header1);
            factory.configurationUpdated(config);
            handler = factory.buildHandler();
            request = mock(HttpServletRequest.class);

            Enumeration<String> e = getEnumeration(requestHeaders);

            when(request.getHeaderNames()).thenReturn(e);
            when(request.getHeader("X-User-Header")).thenReturn("value1");


        }

        /**
         * Test of handleRequest method, of class HeaderTranslatioHandler.
         */
        @Test
        public void shouldRemoveOriginalHeaderAddNewHeaders() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");
            Enumeration<String> b = getEnumeration(requestHeaderValues);
            when(request.getHeaders("X-User-Header")).thenReturn(b);


            myDirector = handler.handleRequest(request, null);

            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header1"));
            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header2"));

            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header1").contains("value1"));
            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header1").contains("value2"));
            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header1").contains("value3"));

            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header2").contains("value1"));
            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header2").contains("value2"));
            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header2").contains("value3"));


            assertEquals("Filter Director should be set to remove original headers",
                    myDirector.requestHeaderManager().headersToRemove().size(), 1);


        }

        @Test
        public void shouldRemoveOriginalHeaderAddNewHeadersSingleValue() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");
            Enumeration<String> b = getEnumeration(singleRequestHeaderValue);
            when(request.getHeaders("X-User-Header")).thenReturn(b);


            myDirector = handler.handleRequest(request, null);

            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header1"));
            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header2"));

            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header1").contains("value1"));

            assertTrue("Filter Director should be set to add all header values", myDirector.requestHeaderManager().headersToAdd().get("new-header2").contains("value1"));

            assertTrue("Filter Director should be set to only add one value for 'new-header1' ", myDirector.requestHeaderManager().headersToAdd().get("new-header1").size() == 1);
            assertTrue("Filter Director should be set to only add one value for 'new-header2' ", myDirector.requestHeaderManager().headersToAdd().get("new-header2").size() == 1);


            assertEquals("Filter Director should be set to remove original headers",
                    myDirector.requestHeaderManager().headersToRemove().size(), 1);

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
        public void shouldPreserveHeaderOrderWhenTranslatingHeaderValues(){

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");
            Enumeration<String> b = getEnumeration(requestHeaderValues);
            when(request.getHeaders("X-User-Header")).thenReturn(b);


            myDirector = handler.handleRequest(request, null);

            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header1"));
            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header2"));

            Iterator<String> itr1 = myDirector.requestHeaderManager().headersToAdd().get("new-header1").iterator();

            int counter=0;

            while(itr1.hasNext()){
                String headerValue= itr1.next();
                assertTrue("Filter Director should be set to add headers while preserving order", headerValue.equals(requestHeaderValues[counter]));
                counter++;
            }

            Iterator<String> itr2 = myDirector.requestHeaderManager().headersToAdd().get("new-header2").iterator();

            counter=0;

            while(itr1.hasNext()){
                String headerValue= itr1.next();
                assertTrue("Filter Director should be set to add headers while preserving order", headerValue.equals(requestHeaderValues[counter]));
                counter++;
            }

        }


        @Test
        public void shouldNotRemoveAnyHeadersIfNoOriginalHeadersFound() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");
            Enumeration<String> b = getEnumeration(singleRequestHeaderValue);
            when(request.getHeader("X-User-Header")).thenReturn("");
            myDirector = handler.handleRequest(request, null);

            assertEquals("Filter Director should not be set to remove original headers",
                    myDirector.requestHeaderManager().headersToRemove().size(), 0);

            assertEquals("Filter Director should not be set to add any new headers",
                    myDirector.requestHeaderManager().headersToAdd().size(), 0);


        }

    }

    public static class WhenAddingRequestKeepOriginalHeaders {

        private HeaderTranslationHandlerFactory factory;
        private HeaderTranslationHandler handler;
        private HttpServletRequest request;
        private HeaderTranslationType config;
        private Header header1;
        private String requestUri1 = "http://openrepose.org/service/servicection1";
        private String[] requestHeaders = {"X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"};
        private String[] requestHeaderValues = {"value1", "value2", "value3"};

        @Before
        public void setUp() {

            factory = new HeaderTranslationHandlerFactory();

            config = new HeaderTranslationType();


            header1 = new Header();
            header1.setOriginalName("X-User-Header");
            header1.getNewName().add("new-header1");
            header1.getNewName().add("new-header2");
            header1.setRemoveOriginal(Boolean.FALSE);

            config.getHeader().add(header1);
            factory.configurationUpdated(config);
            handler = factory.buildHandler();
            request = mock(HttpServletRequest.class);

            Enumeration<String> e = getEnumeration(requestHeaders);
            Enumeration<String> b = getEnumeration(requestHeaderValues);

            when(request.getHeaderNames()).thenReturn(e);
            when(request.getHeader("X-User-Header")).thenReturn("value1");
            when(request.getHeaders("X-User-Header")).thenReturn(b);


        }

        /**
         * Test of handleRequest method, of class HeaderTranslatioHandler.
         */
        @Test
        public void shouldNotRemoveOriginalHeaderAddNewHeaders() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");

            myDirector = handler.handleRequest(request, null);

            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header1"));
            assertTrue("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header2"));

            assertEquals("Filter Director should be set to remove original headers",
                    myDirector.requestHeaderManager().headersToRemove().size(), 0);


        }


        /**
         * Test of handleRequest method, of class HeaderTranslatioHandler.
         */
        @Test
        public void shouldNotAddHeadersWhenOriginalNotForund() {

            FilterDirector myDirector = new FilterDirectorImpl();

            request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");

            myDirector = handler.handleRequest(request, null);

            assertFalse("Filter Director should be set to add headers",
                    myDirector.requestHeaderManager().headersToAdd().containsKey("new-header1"));


        }


    }

    private static Enumeration<String> getEnumeration(String[] values) {

        final String[] val = values;
        Enumeration<String> b = new Enumeration<String>() {

            int size = Array.getLength(val);
            int cursor;

            @Override
            public boolean hasMoreElements() {
                return (cursor < size);
            }

            @Override
            public String nextElement() {
                return (String) Array.get(val, cursor++);
            }
        };

        return b;

    }
}
