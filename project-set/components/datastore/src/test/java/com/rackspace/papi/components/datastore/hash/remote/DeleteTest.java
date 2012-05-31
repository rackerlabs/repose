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
public class DeleteTest {

    /*
   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectDeletionUrl() throws UnknownHostException {
         final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         assertEquals("Delete command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", deleteCommand.buildRequest().getURI().toString());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOnSuccess() throws Exception {
         final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         final HttpResponse mockedResponse = mock(HttpResponse.class);
         when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 202, "Accepted"));

         assertEquals("Delete command must communicate success on 202", Boolean.TRUE, deleteCommand.handleResponse(mockedResponse));
      }

      @Test
      public void shouldReturnFalseOnFailure() throws Exception {
         final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         final HttpResponse mockedResponse = mock(HttpResponse.class);
         when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "Not Found"));

         assertEquals("Delete command must communicate failure on response != 202", Boolean.FALSE, deleteCommand.handleResponse(mockedResponse));
      }
   }
   * 
   */
}
