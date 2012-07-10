package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class JerseyResponseProcessorTest {

   @Ignore
   public static class IsByteArray extends ArgumentMatcher<byte[]> {

      @Override
      public boolean matches(Object argument) {
         boolean result = (argument instanceof byte[]);

         return result;
      }
   }

   public static byte[] anyByteArray() {
      return argThat(new IsByteArray());
   }

    public static class WhenProcessingResponses {
        private ClientResponse response;
        private HttpServletResponse servletResponse;
        private ServletOutputStream out;
        private JerseyResponseProcessor processor;
        MultivaluedMapImpl headers;
        private InputStream in;

        @Before
        public void setUp() throws IOException {
            headers = new MultivaluedMapImpl();
            headers.add("header1", "value1");
            headers.add("header2", "value21");
            headers.add("header2", "value22");

            response = mock(ClientResponse.class);
            servletResponse = mock(HttpServletResponse.class);
            out = mock(ServletOutputStream.class);
            in = mock(InputStream.class);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getStatus()).thenReturn(200);
            when(response.getEntityInputStream()).thenReturn(in);
            when(servletResponse.getOutputStream()).thenReturn(out);
            processor = new JerseyResponseProcessor(response, servletResponse);
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
                List<String> values = headers.get(header, String.class);
                for (String value: values) {
                    verify(servletResponse).setHeader(header, value);
                }
            }
        }

        @Test
        public void shouldWriteResponse() throws IOException {
            processor.process();
            
            verify(in).read(anyByteArray(), anyInt(), anyInt());
            verify(in).close();
            verify(out).flush();
        }
    }

    public static class WhenProcessingMutableHttpResponses {
        private ClientResponse response;
        private MutableHttpServletResponse servletResponse;
        private ServletOutputStream out;
        private JerseyResponseProcessor processor;
        MultivaluedMapImpl headers;
        private InputStream in;

        @Before
        public void setUp() throws IOException {
            headers = new MultivaluedMapImpl();
            headers.add("header1", "value1");
            headers.add("header2", "value21");
            headers.add("header2", "value22");

            response = mock(ClientResponse.class);
            servletResponse = mock(MutableHttpServletResponse.class);
            out = mock(ServletOutputStream.class);
            in = mock(InputStream.class);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getStatus()).thenReturn(200);
            when(response.getEntityInputStream()).thenReturn(in);
            when(servletResponse.getOutputStream()).thenReturn(out);
            processor = new JerseyResponseProcessor(response, servletResponse);
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
                List<String> values = headers.get(header, String.class);
                for (String value: values) {
                    verify(servletResponse).setHeader(header, value);
                }
            }
        }

        @Test
        public void shouldWriteResponse() throws IOException {
            processor.process();
            
            verify(servletResponse).setInputStream(any(JerseyInputStream.class));
        }
    }
}
