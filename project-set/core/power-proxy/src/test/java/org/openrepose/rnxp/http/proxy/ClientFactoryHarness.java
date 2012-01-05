package org.openrepose.rnxp.http.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.rnxp.http.context.SimpleRequestContext;

import static org.mockito.Mockito.*;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.RequestResponsePair;
import org.openrepose.rnxp.servlet.http.ServletInputStream;
import org.openrepose.rnxp.servlet.http.ServletOutputStreamWrapper;
import org.openrepose.rnxp.servlet.http.detached.AbstractHttpServletResponse;

/**
 *
 * @author zinic
 */
public class ClientFactoryHarness {

   public static void main(String[] args) throws Exception {
      final OutboundCoordinator coordinator = new OutboundCoordinator();

      final PowerProxy mockedPowerProxy = mock(PowerProxy.class);

      final SimpleRequestContext context = new SimpleRequestContext(mockedPowerProxy, coordinator);
      final OriginPipelineFactory originPipelineFactory = new OriginPipelineFactory(context);
      final OriginChannelFactory proxyChannelFactory = new OriginChannelFactory();

      final ExternalConnectionFuture connectionFuture = new NettyOriginConnectionFuture(originPipelineFactory, proxyChannelFactory);

      // Mock out request
      final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
      when(mockedRequest.getHeaderNames()).thenReturn(enumerationOf(new String[]{"Host"}));
      when(mockedRequest.getHeaders(eq("Host"))).thenReturn(enumerationOf(new String[]{"localhost:8080"}));

      when(mockedRequest.getMethod()).thenReturn("GET");
      when(mockedRequest.getRequestURI()).thenReturn("/uri");
      when(mockedRequest.getInputStream()).thenReturn(new ServletInputStream(new ByteArrayInputStream(new byte[0])));

      final RecordingHttpServletResponse recordingResponse = new RecordingHttpServletResponse();
      connectionFuture.connect(new RequestResponsePair(mockedRequest, recordingResponse), new InetSocketAddress("127.0.0.1", 8080));

      System.out.println(new String(recordingResponse.getBytes()));
      System.exit(0);
   }

   public static class RecordingHttpServletResponse extends AbstractHttpServletResponse {

      private ServletOutputStreamWrapper<ByteArrayOutputStream> outputStream;

      public RecordingHttpServletResponse() {
         outputStream = new ServletOutputStreamWrapper<ByteArrayOutputStream>(new ByteArrayOutputStream());
      }

      @Override
      public ServletOutputStream getOutputStream() throws IOException {
         return outputStream;
      }

      public byte[] getBytes() {
         return outputStream.getWrappedOutputStream().toByteArray();
      }
   }

   public static <T> Enumeration<T> enumerationOf(T[] array) {
      return Collections.enumeration(Arrays.asList(array));
   }
}
