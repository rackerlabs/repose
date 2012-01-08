package org.openrepose.rnxp.servlet.http.serializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class ResponseHeadSerializerTest {

   public static class WhenSerializingHttpServletRequestHeads {

      private HttpServletResponse mockedResponse;

      @Before
      public void standUp() {
         mockedResponse = mock(HttpServletResponse.class);
      }

      @Test
      public void shouldProduceCorrectSerializedFormWithNoHeaders() {
         final String expectedSerializedForm = "HTTP/1.1 200\r\n\r\n";

         when(mockedResponse.getStatus()).thenReturn(200);
         when(mockedResponse.getHeaderNames()).thenReturn(Collections.EMPTY_LIST);

         final ResponseHeadSerializer responseHeadSerializer = new ResponseHeadSerializer(mockedResponse);
         final StringBuilder serializedRequest = new StringBuilder();

         int read;
         while ((read = responseHeadSerializer.read()) != -1) {
            serializedRequest.append((char) read);
         }

         assertEquals("Serializer must produce correct output", expectedSerializedForm, serializedRequest.toString());
      }

      @Test
      public void shouldProduceCorrectSerializedFormWithHeaders() {
         final String expectedSerializedForm = "HTTP/1.1 200\r\nContent-Length:100\r\n\r\n";

         when(mockedResponse.getStatus()).thenReturn(200);
         when(mockedResponse.getHeaderNames()).thenReturn(Arrays.asList(new String[] {"Content-Length"}));
         when(mockedResponse.getHeaders(eq("Content-Length"))).thenReturn(Arrays.asList(new String[] {"100"}));

         final ResponseHeadSerializer responseHeadSerializer = new ResponseHeadSerializer(mockedResponse);
         final StringBuilder serializedRequest = new StringBuilder();

         int read;
         while ((read = responseHeadSerializer.read()) != -1) {
            serializedRequest.append((char) read);
         }

         assertEquals("Serializer must produce correct output", expectedSerializedForm, serializedRequest.toString());
      }
   }

   public static <T> Enumeration<T> enumerationOf(T[] array) {
      return Collections.enumeration(Arrays.asList(array));
   }
}
