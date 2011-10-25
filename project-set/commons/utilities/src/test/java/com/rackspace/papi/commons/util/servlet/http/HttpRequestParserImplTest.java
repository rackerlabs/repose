package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.servlet.http.parser.Parser;
import com.rackspace.papi.commons.util.servlet.http.parser.RequestParserFactory;
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
 * @author fran
 */
@RunWith(Enclosed.class)
public class HttpRequestParserImplTest {
    public static class WhenParsingRequest {
        private HttpServletRequest mockedRequest;
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        private Enumeration<String> headerNames;
        private Vector<String> headers = new Vector<String>(2);

        @Before
        public void setup() {
            // Create query parameters map
            List<String> values = new ArrayList<String>();
            values.add("1");
            values.add("2");

            parameterMap.put("A", values.toArray(new String[0]));
            parameterMap.put("B", values.toArray(new String[0]));

            // Create headers
            headers.addAll(Arrays.asList("Accept", "Content-Type"));
            headerNames = headers.elements();

            mockedRequest = mock(HttpServletRequest.class);

            when(mockedRequest.getHeaderNames()).thenReturn(headerNames);
            when(mockedRequest.getParameterMap()).thenReturn(parameterMap);
            when(mockedRequest.getHeader("Accept")).thenReturn("application/xml; q=0.8, text/html");
            when(mockedRequest.getHeader("Content-Type")).thenReturn("application/xml");
            when(mockedRequest.getMethod()).thenReturn("POST");
            when(mockedRequest.getRequestURI()).thenReturn("/request");
            when(mockedRequest.getProtocol()).thenReturn("HTTP/1.1");
        }

        @Test
        public void shouldParseFullRequest() {
            Parser parser = RequestParserFactory.newInstance();

            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "\n" +
                    "<httpx xmlns='http://docs.rackspace.com/httpx/v1.0' \n" +
                    "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
                    "    xsi:schemaLocation='http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd'\n" +
                    "    fidelity=\"HEAD BODY\"><request method=\"POST\" uri=\"/request\" version=\"HTTP/1.1\"><head fidelity=\"URI_DETAIL HEADERS\"><uri-detail><query-parameter name=\"A\"><value>1</value><value>2</value></query-parameter><query-parameter name=\"B\"><value>1</value><value>2</value></query-parameter></uri-detail><headers fidelity=\"*\"><header name=\"Accept\"><value>application/xml; q=0.8, text/html</value></header><header name=\"Content-Type\"><value>application/xml</value></header></headers><body></body>\n" +
                    "    </request>\n" +
                    "</httpx>";

            String actual = parser.parse(mockedRequest);

            assertEquals(expected, actual);
        }

        @Test
        public void shouldParseRequestWithoutHead() {
            HttpServletRequest request = mock(HttpServletRequest.class);

            when(request.getHeaderNames()).thenReturn(new Vector<String>(0).elements());
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/request");
            when(request.getProtocol()).thenReturn("HTTP/1.1");

            Parser parser = RequestParserFactory.newInstance();

            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "\n" +
                    "<httpx xmlns='http://docs.rackspace.com/httpx/v1.0' \n" +
                    "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
                    "    xsi:schemaLocation='http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd'\n" +
                    "    fidelity=\"HEAD BODY\"><request method=\"POST\" uri=\"/request\" version=\"HTTP/1.1\"><body></body>\n" +
                    "    </request>\n" +
                    "</httpx>";

            String actual = parser.parse(request);

            assertEquals(expected, actual);
        }
    }
}
