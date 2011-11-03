package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.ResponseHeadDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class HttpResponseParserTest {
    public static class WhenParsing {
        private HttpServletResponse mockedResponse;

        private Vector<String> headers = new Vector<String>(2);
        private Vector<String> retryAfterValues = new Vector<String>(1);

        private List<MessageDetail> messageFidelity = new ArrayList<MessageDetail>();
        private List<ResponseHeadDetail> headFidelity = new ArrayList<ResponseHeadDetail>();
        private List<String> headersFidelity = new ArrayList<String>();

        @Before
        public void setup() throws IOException {
            // Create headers
            headers.addAll(Arrays.asList("Retry-After"));
            retryAfterValues.addAll(Arrays.asList("This is a valid date"));

            mockedResponse = mock(HttpServletResponse.class);

            when(mockedResponse.getStatus()).thenReturn(200);
            when(mockedResponse.getHeaderNames()).thenReturn(headers);
            when(mockedResponse.getHeaders("Retry-After")).thenReturn(retryAfterValues);

            messageFidelity.add(MessageDetail.BODY);
            messageFidelity.add(MessageDetail.HEAD);

            headFidelity.add(ResponseHeadDetail.HEADERS);

            headersFidelity.add("*");
        }

        @Test
        public void shouldParse() throws IOException {
            Parser parser = ResponseParserFactory.newInstance();
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><httpx xmlns=\"http://docs.rackspace.com/httpx/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd\"><response fidelity=\"BODY HEAD\" status-code=\"200\" version=\"HTTP/1.1\"><head fidelity=\"HEADERS\"><headers fidelity=\"*\"><header name=\"Retry-After\"><value>This is a valid date</value></header></headers></head><body/></response></httpx>";

            InputStream actual = parser.parse(mockedResponse, messageFidelity, headFidelity, headersFidelity, false);

            InputStreamReader reader = new InputStreamReader(actual);
            BufferedReader bufReader = new BufferedReader(reader);

            assertEquals(expected, bufReader.readLine());
        }
    }
}
