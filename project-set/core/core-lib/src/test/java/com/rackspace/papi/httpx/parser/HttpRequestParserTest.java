package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.RequestHeadDetail;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.custommonkey.xmlunit.Diff;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.xml.sax.SAXException;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class HttpRequestParserTest {
    public static class WhenParsing {
        private HttpServletRequest mockedRequest;
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        private Enumeration<String> headerNames;
        private Vector<String> headers = new Vector<String>(2);

        private Enumeration<String> acceptHeaderValues;
        private Vector<String> acceptValues = new Vector<String>(1);

        private Enumeration<String> contentTypeHeaderValues;
        private Vector<String> contentTypeValues = new Vector<String>(1);

        private List<MessageDetail> messageFidelity = new ArrayList<MessageDetail>();
        private List<RequestHeadDetail> headFidelity = new ArrayList<RequestHeadDetail>();
        private List<String> headersFidelity = new ArrayList<String>();

        @Before
        public void setup() throws IOException {
            // Create query parameters map
            List<String> values = new ArrayList<String>();
            values.add("1");
            values.add("2");

            parameterMap.put("A", values.toArray(new String[0]));
            parameterMap.put("B", values.toArray(new String[0]));

            // Create headers
            headers.addAll(Arrays.asList("Accept", "Content-Type"));
            headerNames = headers.elements();

            acceptValues.addAll(Arrays.asList("application/xml; q=0.8, text/html"));
            acceptHeaderValues = acceptValues.elements();

            contentTypeValues.addAll(Arrays.asList("application/xml"));
            contentTypeHeaderValues = contentTypeValues.elements();

            mockedRequest = mock(HttpServletRequest.class);

            when(mockedRequest.getHeaderNames()).thenReturn(headerNames);
            when(mockedRequest.getHeaders("accept")).thenReturn(acceptHeaderValues);
            when(mockedRequest.getHeaders("Content-Type")).thenReturn(contentTypeHeaderValues);
            when(mockedRequest.getParameterMap()).thenReturn(parameterMap);
            when(mockedRequest.getHeader("accept")).thenReturn("application/xml; q=0.8, text/html");
            when(mockedRequest.getMethod()).thenReturn("POST");
            when(mockedRequest.getRequestURI()).thenReturn("/request");
            when(mockedRequest.getProtocol()).thenReturn("HTTP/1.1");
            when(mockedRequest.getContentType()).thenReturn("application/xml");
            when(mockedRequest.getHeader(eq("content-type"))).thenReturn("application/xml");
            
            messageFidelity.add(MessageDetail.BODY);
            messageFidelity.add(MessageDetail.HEAD);

            headFidelity.add(RequestHeadDetail.URI_DETAIL);
            headFidelity.add(RequestHeadDetail.HEADERS);

            headersFidelity.add("*");
            headersFidelity.add("ACCEPT");
        }

        @Test
        public void shouldParse() throws IOException, SAXException {
            Parser parser = RequestParserFactory.newInstance();
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><httpx xmlns=\"http://docs.rackspace.com/httpx/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd\"><request fidelity=\"BODY HEAD\" uri=\"/request\" method=\"POST\" version=\"HTTP/1.1\"><head fidelity=\"URI_DETAIL HEADERS\"><uri-detail fragment=\"where do we get this?\"><query-parameter name=\"A\"><value>1</value><value>2</value></query-parameter><query-parameter name=\"B\"><value>1</value><value>2</value></query-parameter></uri-detail><headers fidelity=\"* ACCEPT\"><accept><media-range subtype=\"xml\" type=\"application\"><parameter value=\"0.8\" name=\"q\"/></media-range><media-range subtype=\"html\" type=\"text\"/></accept><header name=\"Content-Type\"><value>application/xml</value></header></headers></head><body/></request></httpx>";

            InputStream actual = parser.parse(mockedRequest, messageFidelity, headFidelity, headersFidelity, false);

            InputStreamReader reader = new InputStreamReader(actual);
            BufferedReader bufReader = new BufferedReader(reader);
            
            Diff diff = new Diff(expected, bufReader.readLine());

            assertTrue("XML should be equivalent", diff.similar());
            //assertEquals(expected, bufReader.readLine());
        }
    }
}
