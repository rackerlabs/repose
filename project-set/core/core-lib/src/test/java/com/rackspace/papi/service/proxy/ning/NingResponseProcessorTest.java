package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class NingResponseProcessorTest {
    
    public static class WhenProcessingResults {
        private Response response;
        private HttpServletResponse servletResponse;
        private FluentCaseInsensitiveStringsMap headers;
        private ServletOutputStream out;
        private InputStream in;
        private NingResponseProcessor processor;
        @Before
        public void setup() throws IOException {
            response = mock(Response.class);
            servletResponse = mock(HttpServletResponse.class);
            headers = new FluentCaseInsensitiveStringsMap();
            headers.add("header1", "value1");
            headers.add("header2", "value21");
            headers.add("header2", "value22");

            out = mock(ServletOutputStream.class);
            in = mock(InputStream.class);
            when(response.getStatusCode()).thenReturn(200);
            when(response.getHeaders()).thenReturn(headers);
            when(servletResponse.getOutputStream()).thenReturn(out);
            
            processor = new NingResponseProcessor(response, servletResponse);

        }

        @Test
        public void shouldSetStatus() throws IOException {
            processor.process();
            verify(servletResponse).setStatus(eq(200));
        }

        @Test
        public void shouldSetHeaders() throws IOException {
            processor.process();
            
            verify(servletResponse).setStatus(eq(200));
            for (String header: headers.keySet()) {
                List<String> values = headers.get(header);
                for (String value: values) {
                    verify(servletResponse).addHeader(header, value);
                }
            }
        }

    }
}
