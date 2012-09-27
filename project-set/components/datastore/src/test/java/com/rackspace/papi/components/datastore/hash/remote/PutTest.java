package com.rackspace.papi.components.datastore.hash.remote;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.junit.Ignore;

/**
 *
 * @author zinic
 */
@Ignore
@RunWith(Enclosed.class)
public class PutTest {
/*
   public static final int TTL = 20;

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectPutUrl() throws UnknownHostException {
         final Put getCommand = new Put(TimeUnit.SECONDS, new byte[2], TTL, "object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         assertEquals("Put command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", getCommand.buildRequest().getURI().toString());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOn202() throws Exception {
         final Put putCommand = new Put(TimeUnit.SECONDS, new byte[2], TTL, "object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         final HttpResponse mockedResponse = mock(HttpResponse.class);
         when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 202, "Accepted"));

         assertEquals("Successful put must return true", Boolean.TRUE, putCommand.handleResponse(mockedResponse));
      }

      @Test(expected = DatastoreOperationException.class)
      public void shouldCommunicateFailureAsException() throws Exception {
         final Put putCommand = new Put(TimeUnit.SECONDS, new byte[2], TTL, "object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         final HttpResponse mockedResponse = mock(HttpResponse.class);
         when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 500, "Lewz"));

         putCommand.handleResponse(mockedResponse);
      }
   }
   * 
   */
}
