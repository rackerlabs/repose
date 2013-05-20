package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.api.client.PartialRequestBuilder;
import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
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
        private String[] params = {"param1", "param2"};
        private String[] params1 = {"value1"};
        private String[] params2 = {"value21", "value22"};
        private PartialRequestBuilder builder;
        private ServletInputStream input;
        private String queryString;

        @Before
        public void setUp() throws IOException, URISyntaxException {
            request = mock(HttpServletRequest.class);
            uri = new URI("http://www.openrepose.org"); // mock(URI.class);
            builder = mock(PartialRequestBuilder.class);
            input = mock(ServletInputStream.class);
            
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
            
            processor = new JerseyRequestProcessor(request, new URI("www.openrepose.org"), true);
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
        public void shouldSetEmptyAcceptHeader() throws IOException{
           
           when(input.read()).thenReturn(-1);
            processor.process(builder);
            
            verify(request).getHeaderNames();
            
            verify(builder).accept("");
        }
        
        @Test
        public void shouldSetEmptyAcceptHeaderOnEmptyAcceptCollections() throws IOException{
           
           when(request.getHeaders(eq("accept"))).thenReturn(Collections.enumeration(Arrays.asList("")));
           when(input.read()).thenReturn(-1);
            processor.process(builder);
            
            verify(request).getHeaderNames();
            
            verify(builder).accept("");
        }

        @Test
        public void shouldSetInputStream() throws IOException {
            when(input.read()).thenReturn((int)'1');
            processor.process(builder);
            
            verify(builder).entity(any(PushbackInputStream.class));
        }
        
        @Test
        public void shouldSetRequestParams() {
            WebResource resource = mock(WebResource.class);
            when(resource.queryParam(anyString(), anyString())).thenReturn(resource);
            
            processor.setRequestParameters(resource);
            verify(request).getQueryString();
            for (String param: params1) {
                verify(resource).queryParam(eq("param1"), eq(param));
            }

            for (String param: params2) {
                verify(resource).queryParam(eq("param2"), eq(param));
            }
        }
    }

}
