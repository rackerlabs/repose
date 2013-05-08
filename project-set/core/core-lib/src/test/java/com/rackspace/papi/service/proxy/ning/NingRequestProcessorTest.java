package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.BodyGenerator;
import com.ning.http.client.RequestBuilder;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.commons.util.proxy.TargetHostInfo;
import com.rackspace.papi.service.proxy.ning.NingRequestProcessor.RequestBody;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class NingRequestProcessorTest {

    public static class WhenProcessingRequests {

        private URI uri;
        private HttpServletRequest request;
        private NingRequestProcessor processor;
        private String[] headers = {"header1", "header2"};
        private String[] values1 = {"value1"};
        private String[] values2 = {"value21", "value22"};
        private String[] params = {"param1", "param2"};
        private String[] params1 = {"value1"};
        private String[] params2 = {"value21", "value22"};
        private ServletInputStream input;
        private TargetHostInfo host;
        private RequestBuilder builder;
        private String queryString;

        @Before
        public void setUp() throws URISyntaxException, IOException {
            host = new TargetHostInfo("http://www.openrepose.org");
            request = mock(HttpServletRequest.class);
            uri = new URI("http://www.openrepose.org"); // mock(URI.class);
            input = mock(ServletInputStream.class);
            builder = mock(RequestBuilder.class);
            queryString = "param1=value1&param2=value21&param2=value22&param2=";

            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(headers)));
            when(request.getHeaders(eq("header1"))).thenReturn(Collections.enumeration(Arrays.asList(values1)));
            when(request.getHeaders(eq("header2"))).thenReturn(Collections.enumeration(Arrays.asList(values2)));
            when(request.getParameterNames()).thenReturn(Collections.enumeration(Arrays.asList(params)));
            when(request.getParameterValues(eq("param1"))).thenReturn(params1);
            when(request.getParameterValues(eq("param2"))).thenReturn(params2);
            when(request.getQueryString()).thenReturn(queryString);
            when(request.getInputStream()).thenReturn(input);
            when(request.getMethod()).thenReturn("POST");

            processor = new NingRequestProcessor(request, host, builder);
        }

        @Test
        public void shouldSetHeaders() throws IOException {

            when(input.read()).thenReturn(-1);
            processor.process();

            verify(request).getHeaderNames();
            for (String header : headers) {
                verify(request).getHeaders(eq(header));
            }

            for (String value : values1) {
                verify(builder).addHeader(eq("header1"), eq(value));
            }

            for (String value : values2) {
                verify(builder).addHeader(eq("header2"), eq(value));
            }
        }

        @Test
        public void shouldSetParams() throws IOException {

            when(input.read()).thenReturn(-1);
            processor.process();

            verify(request).getQueryString();

            for (String param : params1) {
                verify(builder).addQueryParameter(eq("param1"), eq(param));
            }

            for (String param : params2) {
                verify(builder).addQueryParameter(eq("param2"), eq(param));
            }
        }

        @Test
        public void shouldSetBody() throws IOException {
            when(input.read()).thenReturn(-1);
            processor.process();
            verify(builder).setBody(any(BodyGenerator.class));
        }
    }

    private static class DataInputStream extends ServletInputStream {
        private final InputStream in;

        DataInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }
    }

    public static class WhenProcessingRequestBody {

        private static final byte[] inData = "Some Data".getBytes();
        private ByteBuffer data;
        private HttpServletRequest request;
        private DataInputStream in;
        private RequestBody bodyReader;

        @Before
        public void setup() throws IOException {
            data = ByteBuffer.allocate(inData.length);
            request = mock(HttpServletRequest.class);
            in = new DataInputStream(new ByteArrayInputStream(inData));

            when(request.getInputStream()).thenReturn(in);
            bodyReader = new NingRequestProcessor.RequestBody(request);
        }
        
        @Test
        public void shoudGetRequestStream() throws IOException {
            verify(request).getInputStream();
        }
        
        @Test
        public void shouldTryToReadBufferUpToCapacity() throws IOException {
            assertTrue(data.hasRemaining());
            bodyReader.read(data);
            assertFalse(data.hasRemaining());
            for (int i = 0; i < inData.length; i++) {
                assertEquals(inData[i], data.array()[i]);
            }
        }
    }
}
