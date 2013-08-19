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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author kush5342
 */
@RunWith(Enclosed.class)
public class HeaderTranslationHandlerTest {

    public static class WhenAddingRequestHeaders {

        private HeaderTranslationHandlerFactory factory;
        private HeaderTranslationHandler handler;
        private HttpServletRequest request;
        private HeaderTranslationType config;
        private Header header1;
        private String requestUri1 = "http://openrepose.org/service/servicection1"; 
        private String[] requestHeaders = {"X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"};
  
        @Before
        public void setUp() {

            factory = new HeaderTranslationHandlerFactory();
           
            config = new HeaderTranslationType();
           
         
            header1=new Header();
            header1.setOriginalName("X-User-Header");
            header1.getNewName().add("New-Header1");
            header1.getNewName().add("New-Header2");
            header1.setRemoveOriginal(Boolean.TRUE);
            
            config.getHeader().add(header1);
            factory.configurationUpdated(config);
            handler = factory.buildHandler();
            request = mock(HttpServletRequest.class);

             Enumeration<String> e = new Enumeration<String>() {

                int size = Array.getLength(requestHeaders);
                int cursor;

                @Override
                public boolean hasMoreElements() {
                    return (cursor < size);
                }

                @Override
                public String nextElement() {
                    return (String) Array.get(requestHeaders, cursor++);
                }
            };
            when(request.getHeaderNames()).thenReturn(e);


        }

        /**
         * Test of handleRequest method, of class HeaderTranslatioHandler.
         */
        @Test
        public void shouldRemoveOriginalHeaderAddNewHeaders() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");

            myDirector = handler.handleRequest(request, null);

            assertTrue("Filter Director should be set to add headers", 
                    myDirector.requestHeaderManager().headersToAdd().containsKey("New-Header1"));
            assertTrue("Filter Director should be set to add headers", 
                    myDirector.requestHeaderManager().headersToAdd().containsKey("New-Header2"));
            
            assertEquals("Filter Director should be set to remove original headers", 
                    myDirector.requestHeaderManager().headersToRemove().size(), 1);

        }


    }
}
