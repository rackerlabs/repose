package org.openrepose.rnxp.servlet.http.serializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
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
public class RequestHeadSerializerTest {

   public static class WhenSerializingHttpServletRequestHeads {

      private HttpServletRequest mockedRequest;

      @Before
      public void standUp() {
         mockedRequest = mock(HttpServletRequest.class);
         when(mockedRequest.getMethod()).thenReturn("GET");
         when(mockedRequest.getRequestURI()).thenReturn("/uri");
      }

      @Test
      public void shouldProduceCorrectSerializedFormWithNoHeaders() {
         final String expectedSerializedForm = "GET /uri HTTP/1.1\r\n\r\n";

         final Enumeration<String> mockEnumeration = mock(Enumeration.class);
         when(mockEnumeration.hasMoreElements()).thenReturn(false);

         when(mockedRequest.getHeaderNames()).thenReturn(mockEnumeration);

         final RequestHeadSerializer serializer = new RequestHeadSerializer(mockedRequest);
         final StringBuilder serializedRequest = new StringBuilder();

         int read;
         while ((read = serializer.read()) != -1) {
            serializedRequest.append((char) read);
         }

         assertEquals("Serializer must produce correct output", expectedSerializedForm, serializedRequest.toString());
      }

      @Test
      public void shouldProduceCorrectSerializedFormWithQueryParameters() {
         final String expectedSerializedForm = "GET /uri?test=true HTTP/1.1\r\n\r\n";

         final Enumeration<String> mockEnumeration = mock(Enumeration.class);
         when(mockEnumeration.hasMoreElements()).thenReturn(false);

         when(mockedRequest.getHeaderNames()).thenReturn(mockEnumeration);
         when(mockedRequest.getQueryString()).thenReturn("test=true");

         final RequestHeadSerializer serializer = new RequestHeadSerializer(mockedRequest);
         final StringBuilder serializedRequest = new StringBuilder();

         int read;
         while ((read = serializer.read()) != -1) {
            serializedRequest.append((char) read);
         }

         assertEquals("Serializer must produce correct output", expectedSerializedForm, serializedRequest.toString());
      }

      @Test
      public void shouldProduceCorrectSerializedFormWithHeaders() {
         final String expectedSerializedForm = "GET /uri HTTP/1.1\r\nHost:localhost:8080\r\nContent-Type:application/xml\r\n\r\n";

         when(mockedRequest.getHeaderNames()).thenReturn(enumerationOf(new String[]{"Host", "Content-Type"}));
         when(mockedRequest.getHeaders(eq("Host"))).thenReturn(enumerationOf(new String[]{"localhost:8080"}));
         when(mockedRequest.getHeaders(eq("Content-Type"))).thenReturn(enumerationOf(new String[]{"application/xml"}));

         final RequestHeadSerializer serializer = new RequestHeadSerializer(mockedRequest);
         final StringBuilder serializedRequest = new StringBuilder();

         int read;
         while ((read = serializer.read()) != -1) {
            serializedRequest.append((char) read);
         }

         assertEquals("Serializer must produce correct output", expectedSerializedForm, serializedRequest.toString());
      }
   }

   public static <T> Enumeration<T> enumerationOf(T[] array) {
      return Collections.enumeration(Arrays.asList(array));
   }
}
