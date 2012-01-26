package com.rackspace.papi.commons.util.io.stream;

import java.io.IOException;
import java.io.InputStream;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ServletInputStreamWrapperTest {

   public static class WhenReadingStream {
      InputStream stream1;
      ServletInputStreamWrapper wrapper;

      @Before
      public void setUp() {
         stream1 = mock(InputStream.class);
         wrapper = new ServletInputStreamWrapper(stream1);
      }
      
      @Test
      public void shouldReadFromInputStream() throws IOException {
         wrapper.read();
         verify(stream1).read();
      }
   }
}
