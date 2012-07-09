package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.api.client.PartialRequestBuilder;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class JerseyRequestProcessorTest {

    public static class WhenProcessingRequests {
        private URI uri;
        private HttpServletRequest request;
        private JerseyRequestProcessor processor;
        private String[] headers = {"header1", "header2"};
        private String[] values1 = {"value1"};
        private String[] values2 = {"value21", "value22"};
        private PartialRequestBuilder builder;
        private ServletInputStream input;

        @Before
        public void setUp() throws IOException, URISyntaxException {
            request = mock(HttpServletRequest.class);
            uri = new URI("http://www.openrepose.org"); // mock(URI.class);
            builder = mock(PartialRequestBuilder.class);
            input = mock(ServletInputStream.class);
            
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(headers)));
            when(request.getHeaders(eq("header1"))).thenReturn(Collections.enumeration(Arrays.asList(values1)));
            when(request.getHeaders(eq("header2"))).thenReturn(Collections.enumeration(Arrays.asList(values2)));
            when(request.getInputStream()).thenReturn(input);
            
            processor = new JerseyRequestProcessor(request, uri);
        }
        
        @Test
        public void shouldSetHeaders() throws IOException {
            when(input.read()).thenReturn(-1);
            processor.process(builder);
            
            verify(request).getHeaderNames();
            for (String header: headers) {
                verify(request).getHeaders(eq(header));
            }
            
            for (String value: values1) {
                verify(builder).header(eq("header1"), eq(value));
            }

            for (String value: values2) {
                verify(builder).header(eq("header2"), eq(value));
            }
        }

        @Test
        public void shouldSetInputStream() throws IOException {
            when(input.read()).thenReturn((int)'1');
            processor.process(builder);
            
            verify(builder).entity(any(PushbackInputStream.class));
        }
    }

}
