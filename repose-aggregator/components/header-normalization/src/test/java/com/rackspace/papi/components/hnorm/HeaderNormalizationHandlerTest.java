package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.*;
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
 * @author malconis
 */
@RunWith(Enclosed.class)
public class HeaderNormalizationHandlerTest {

    public static class WhenWhiteListingRequestHeaders {

        private HeaderNormalizationHandlerFactory factory;
        private HeaderNormalizationHandler handler;
        private HttpServletRequest request;
        private HeaderNormalizationConfig config;
        private HeaderFilterList headerFilterList;
        private Target target;
        private HttpHeaderList whitelist;
        private HttpHeader header1;
        private HttpHeader header2;
        private HttpHeader header3;
        private String hv1 = "X-Auth-Token";
        private String hv2 = "X-User-Header";
        private String hv3 = "X-Something";
        private String uriRegex = ".*/service/(.*)";
        private String requestUri1 = "http://openrepose.org/service/servicection1";
        private String requestUri2 = "http://openrepose.org/resource/servicection1";
        private String[] requestHeaders = {"X-Auth-Header", "Content-Type", "X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"};

        @Before
        public void setUp() {

            factory = new HeaderNormalizationHandlerFactory(null);
            headerFilterList = new HeaderFilterList();
            whitelist = new HttpHeaderList();
            header1 = new HttpHeader();
            header3 = new HttpHeader();
            header2 = new HttpHeader();
            config = new HeaderNormalizationConfig();

            target = new Target();

            header1.setId(hv1);
            whitelist.getHeader().add(header1);

            header2.setId(hv2);
            whitelist.getHeader().add(header2);

            header3.setId(hv3);
            whitelist.getHeader().add(header3);

            target.getWhitelist().add(whitelist);
            target.setUriRegex(uriRegex);
            target.getHttpMethods().add(HttpMethod.GET);


            headerFilterList.getTarget().add(target);

            config.setHeaderFilters(headerFilterList);

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
         * Test of handleRequest method, of class HeaderNormalizationHandler.
         */
        @Test
        public void shouldWhiteListRequestsMatchingUriAndMethod() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");

            myDirector = handler.handleRequest(request, null);

            assertFalse("Filter Director should be set to remove headers", myDirector.requestHeaderManager().headersToRemove().isEmpty());
            assertTrue("Filter Director should be set to remove the 'X-Group-Header'", myDirector.requestHeaderManager().headersToRemove().contains("x-group-header"));
            assertEquals("Filter Director should be set to remove 4 headers", myDirector.requestHeaderManager().headersToRemove().size(), 4);

        }

        @Test
        public void shouldNotWhiteListRequestNotMatchingMethod() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("DELETE");

            myDirector = handler.handleRequest(request, null);

            assertTrue(myDirector.requestHeaderManager().headersToRemove().isEmpty());
        }

        @Test
        public void shouldNotWhiteListRequestNotMatchingUri() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri2);
            when(request.getMethod()).thenReturn("DELETE");

            myDirector = handler.handleRequest(request, null);

            assertTrue(myDirector.requestHeaderManager().headersToRemove().isEmpty());
        }
    }

    public static class WhenBlackListingHeaders {

        private HeaderNormalizationHandlerFactory factory;
        private HeaderNormalizationHandler handler;
        private HttpServletRequest request;
        private HeaderNormalizationConfig config;
        private HeaderFilterList headerFilterList;
        private Target target;
        private HttpHeaderList blacklist;
        private HttpHeader header1;
        private HttpHeader header2;
        private HttpHeader header3;
        private String hv1 = "X-Auth-Token";
        private String hv2 = "X-User-Header";
        private String hv3 = "X-Something";
        private String uriRegex = ".*/service/(.*)";
        private String requestUri1 = "http://openrepose.org/service/servicection1";
        private String requestUri2 = "http://openrepose.org/resource/servicection1";
        private String[] requestHeaders = {"X-Auth-Header", "Content-Type", "X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"};

        @Before
        public void setUp() {

            factory = new HeaderNormalizationHandlerFactory(null);
            headerFilterList = new HeaderFilterList();
            blacklist = new HttpHeaderList();
            header1 = new HttpHeader();
            header3 = new HttpHeader();
            header2 = new HttpHeader();
            config = new HeaderNormalizationConfig();

            target = new Target();

            header1.setId(hv1);
            blacklist.getHeader().add(header1);

            header2.setId(hv2);
            blacklist.getHeader().add(header2);

            header3.setId(hv3);
            blacklist.getHeader().add(header3);

            target.getBlacklist().add(blacklist);
            target.setUriRegex(uriRegex);
            target.getHttpMethods().add(HttpMethod.GET);


            headerFilterList.getTarget().add(target);

            config.setHeaderFilters(headerFilterList);

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
         * Test of handleRequest method, of class HeaderNormalizationHandler.
         */
        @Test
        public void shouldWhiteListRequestsMatchingUriAndMethod() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");

            myDirector = handler.handleRequest(request, null);

            assertFalse(myDirector.requestHeaderManager().headersToRemove().isEmpty());
            assertFalse(myDirector.requestHeaderManager().headersToRemove().contains("X-Group-Header"));
            assertEquals("Filter Director should be set to remove 2 headers", myDirector.requestHeaderManager().headersToRemove().size(), 2);


        }

        @Test
        public void shouldNotWhiteListRequestNotMatchingMethod() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("DELETE");

            myDirector = handler.handleRequest(request, null);

            assertTrue(myDirector.requestHeaderManager().headersToRemove().isEmpty());
        }

        @Test
        public void shouldNotWhiteListRequestNotMatchingUri() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri2);
            when(request.getMethod()).thenReturn("DELETE");

            myDirector = handler.handleRequest(request, null);

            assertTrue(myDirector.requestHeaderManager().headersToRemove().isEmpty());
        }
    }

    public static class WhenWhiteListingWithOutUriRegex {

        private HeaderNormalizationHandlerFactory factory;
        private HeaderNormalizationHandler handler;
        private HttpServletRequest request;
        private HeaderNormalizationConfig config;
        private HeaderFilterList headerFilterList;
        private Target target1,target2;
        private HttpHeaderList whitelist1,whitelist2;
        private HttpHeader header1, header2, header3, header4, header5;
        private String hv1 = "X-Auth-Token";
        private String hv2 = "X-User-Header";
        private String hv3 = "X-Something";
        private String hv4 = "X-Group-Header";
        private String hv5 = "Accept";
        private String uriRegex = ".*/service/(.*)";
        private String requestUri1 = "http://openrepose.org/resource/servicection1";
        private String[] requestHeaders = {"X-Auth-Header", "Content-Type", "X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"};

        @Before
        public void setUp() {

            factory = new HeaderNormalizationHandlerFactory(null);
            headerFilterList = new HeaderFilterList();
            whitelist1 = new HttpHeaderList();
            whitelist2 = new HttpHeaderList();
            header1 = new HttpHeader();
            header3 = new HttpHeader();
            header2 = new HttpHeader();
            header4 = new HttpHeader();
            header5 = new HttpHeader();
            config = new HeaderNormalizationConfig();

            target1 = new Target();
            target2 = new Target();

            header1.setId(hv1);
            whitelist1.getHeader().add(header1);

            header2.setId(hv2);
            whitelist1.getHeader().add(header2);

            header3.setId(hv3);
            whitelist1.getHeader().add(header3);

            target1.getWhitelist().add(whitelist1);
            target1.setUriRegex(uriRegex);
            
            header4.setId(hv4);
            whitelist2.getHeader().add(header4);
            
            header5.setId(hv5);
            whitelist2.getHeader().add(header5);
            
            target2.getWhitelist().add(whitelist2);
            target2.getHttpMethods().add(HttpMethod.GET);


            headerFilterList.getTarget().add(target1);
            headerFilterList.getTarget().add(target2);

            config.setHeaderFilters(headerFilterList);

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
         * Test of handleRequest method, of class HeaderNormalizationHandler.
         */
        @Test
        public void shouldWhiteListRequestsWithCatchAllUri() {

            FilterDirector myDirector = new FilterDirectorImpl();
            when(request.getRequestURI()).thenReturn(requestUri1);
            when(request.getMethod()).thenReturn("GET");

            myDirector = handler.handleRequest(request, null);

            assertFalse("Filter Director should be set to remove headers", myDirector.requestHeaderManager().headersToRemove().isEmpty());
            assertTrue("Filter Director should be set to remove the 'X-Auth-Token'", myDirector.requestHeaderManager().headersToRemove().contains("x-auth-token"));
            assertEquals("Filter Director should be set to remove 4 headers", myDirector.requestHeaderManager().headersToRemove().size(), 4);

        }

    }
    
    
}
