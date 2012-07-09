package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.api.client.ClientResponse;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class JerseyInputStreamTest {

    public static class WhenReadingNullInputStream {
        JerseyInputStream input;
        ClientResponse response;
        
        @Before
        public void setUp() {
            response = mock(ClientResponse.class);
            when(response.getEntityInputStream()).thenReturn(null);
            input = new JerseyInputStream(response);
        }
        
        @Test
        public void shouldHandleNullInputStream() throws IOException {
            final int expected = -1;
            
            
            assertEquals("Should return -1 when reading a null input stream", expected, input.read());
            assertEquals("Should return -1 when reading a null input stream", expected, input.read(new byte[10]));
            assertEquals("Should return -1 when reading a null input stream", expected, input.read(new byte[10], 0, 5));
            assertEquals("Should return -1 when skipping a null input stream", expected, input.skip(1));
            assertFalse("Should not support marking a null stream", input.markSupported());
            
            assertEquals("Should return 0 available when reading null input stream", 0, input.available());
            
            input.close();
            verify(response).close();
        }
        
    }

    public static class WhenReadingNonNullInputStream {
        JerseyInputStream input;
        ClientResponse response;
        InputStream stream;
        
        @Before
        public void setUp() {
            response = mock(ClientResponse.class);
            stream = mock(InputStream.class);
            
            when(response.getEntityInputStream()).thenReturn(stream);
            
            input = new JerseyInputStream(response);
        }
        
        @Test
        public void shouldCallUnderlyingStream() throws IOException {
            
            input.read();
            verify(stream).read();
            
            byte[] bytes = new byte[10];
            
            input.read(bytes);
            verify(stream).read(bytes);

            input.read(bytes, 0, 5);
            verify(stream).read(bytes, 0, 5);
            
            input.markSupported();
            verify(stream).markSupported();
            
            input.mark(5);
            verify(stream).mark(5);
            
            input.close();
            verify(stream).close();
            
            input.available();
            verify(stream).available();
            
            input.reset();
            verify(stream).reset();
            
            
        }
    }

}
