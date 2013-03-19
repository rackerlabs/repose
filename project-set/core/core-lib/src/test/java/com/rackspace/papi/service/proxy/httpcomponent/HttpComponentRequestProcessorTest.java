package com.rackspace.papi.service.proxy.httpcomponent;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class HttpComponentRequestProcessorTest {

    public static class WhenProcessingRequests {
        private URI uri;
        private HttpServletRequest request;
        private HttpComponentRequestProcessor processor;
        private String[] headers = {"header1", "header2"};
        private String[] values1 = {"value1"};
        private String[] values2 = {"value21", "value22"};
        private String[] params = {"param1", "param2"};
        private String[] params1 = {"value1"};
        private String[] params2 = {"value21", "value22"};
        private ServletInputStream input;
        private HttpHost host;
        private HttpEntityEnclosingRequestBase method;
        private HttpParams methodParams;

        @Before
        public void setUp() throws URISyntaxException, IOException {
            request = mock(HttpServletRequest.class);
            uri = new URI("http://www.openrepose.org"); // mock(URI.class);
            input = mock(ServletInputStream.class);
            host = new HttpHost("somename");
            method = mock(HttpEntityEnclosingRequestBase.class);
            methodParams = mock(HttpParams.class);
            
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(headers)));
            when(request.getHeaders(eq("header1"))).thenReturn(Collections.enumeration(Arrays.asList(values1)));
            when(request.getHeaders(eq("header2"))).thenReturn(Collections.enumeration(Arrays.asList(values2)));
            when(request.getParameterNames()).thenReturn(Collections.enumeration(Arrays.asList(params)));
            when(request.getParameterValues(eq("param1"))).thenReturn(params1);
            when(request.getParameterValues(eq("param2"))).thenReturn(params2);
            when(request.getInputStream()).thenReturn(input);
            when(method.getParams()).thenReturn(methodParams);
            processor = new HttpComponentRequestProcessor(request, new URI("www.openrepose.org"), true);
        }

        @Test
        public void shouldSetHeaders() throws IOException {
            
            when(input.read()).thenReturn(-1);
            processor.process(method);
            
            verify(request).getHeaderNames();
            for (String header: headers) {
                verify(request).getHeaders(eq(header));
            }
            
            for (String value: values1) {
                verify(method).addHeader(eq("header1"), eq(value));
            }

            for (String value: values2) {
                verify(method).addHeader(eq("header2"), eq(value));
            }
        }

        @Test
        @Ignore
        public void shouldSetParams() throws IOException {
            
            when(input.read()).thenReturn(-1);
            processor.process(method);
            
            verify(request).getParameterNames();
            for (String param: params) {
                verify(request).getParameterValues(eq(param));
            }

            verify(method, times(2)).setParams(any(BasicHttpParams.class));
            /*
            for (String param: params1) {
                verify(methodParams).setParameter(eq("param1"), eq(param));
            }

            for (String param: params2) {
                verify(methodParams).setParameter(eq("param2"), eq(param));
            }
            */
        }

        @Test
        public void shouldSetInputStream() throws IOException {
            when(input.read()).thenReturn((int)'1');
            processor.process(method);
            
            verify(method).setEntity(any(InputStreamEntity.class));
        }
    }
}
