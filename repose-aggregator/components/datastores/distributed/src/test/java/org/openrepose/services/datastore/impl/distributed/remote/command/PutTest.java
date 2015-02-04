package org.openrepose.services.datastore.impl.distributed.remote.command;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.services.datastore.DatastoreOperationException;
import org.openrepose.services.datastore.impl.distributed.CacheRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class PutTest {

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectPutUrl() throws UnknownHostException {
         //final Put putCommand = new Put("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
         final String putData = "Put data";
         final int ttl = 30;
         final String key = "someKey";
         final Put putCommand = new Put(TimeUnit.MINUTES, putData, ttl, key, new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         Assert.assertEquals("Put command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + key, putCommand.getUrl());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOnSuccess() throws Exception {
         final String putData = "Put data";
         final int ttl = 30;
         final Put putCommand = new Put(TimeUnit.MINUTES, putData, ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         // RemoteBehavior.ALLOW_FORWARDING
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         final String responseData = "Response Data";

         ByteArrayInputStream bt = new ByteArrayInputStream(ObjectSerializer.instance().writeObject(responseData));

         when(response.getData()).thenReturn(bt);
         when(response.getStatus()).thenReturn(202);

         Assert.assertEquals("Put command must communicate success on 202", Boolean.TRUE, putCommand.handleResponse(response));
      }

      
      @Test(expected = DatastoreOperationException.class)
      public void shouldThrowExeptionOnUnauthorized() throws Exception {
         final String putData = "Put data";
         final int ttl = 30;
         final Put putCommand = new Put(TimeUnit.MINUTES, putData, ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         when(response.getStatus()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);

         putCommand.handleResponse(response);
      }
   }
}
