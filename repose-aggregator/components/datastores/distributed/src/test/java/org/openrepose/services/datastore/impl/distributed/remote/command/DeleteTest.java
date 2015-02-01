package org.openrepose.services.datastore.impl.distributed.remote.command;

import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.services.datastore.impl.distributed.CacheRequest;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class DeleteTest {

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectDeletionUrl() throws UnknownHostException {
         final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         assertEquals("Delete command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", deleteCommand.getUrl());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOnSuccess() throws Exception {
         final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         // RemoteBehavior.ALLOW_FORWARDING
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         when(response.getStatus()).thenReturn(202);

         assertEquals("Delete command must communicate success on 202", Boolean.TRUE, deleteCommand.handleResponse(response));
      }

      @Test
      public void shouldReturnFalseOnFailure() throws Exception {
         final Delete deleteCommand = new Delete("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         when(response.getStatus()).thenReturn(404);

         assertEquals("Delete command must communicate failure on response != 202", Boolean.FALSE, deleteCommand.handleResponse(response));
      }
   }
}
