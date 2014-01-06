package com.rackspace.papi.components.datastore.impl.distributed.remote.command;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.impl.distributed.CacheRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class PatchTest {

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectPatchUrl() throws UnknownHostException {
         //final Patch patchCommand = new Patch("object-key",
         // new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
         final String patchData = "Patch data";
         final int ttl = 30;
         final String key = "someKey";
         final Patch patchCommand = new Patch(TimeUnit.MINUTES,
                 patchData.getBytes(), ttl, key,
                 new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         Assert.assertEquals("Get command must target expected URL", "http://127.0.0.1:1000" +
                 CacheRequest.CACHE_URI_PATH + key, patchCommand.getUrl());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOnSuccess() throws Exception {
         final String patchData = "Patch data";
         final int ttl = 30;
         final Patch patchCommand = new Patch(TimeUnit.MINUTES, patchData.getBytes(),
                 ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         // RemoteBehavior.ALLOW_FORWARDING
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         final String responseData = "Response Data";

         ByteArrayInputStream bt = new ByteArrayInputStream(responseData.getBytes("UTF-8"));

         when(response.getData()).thenReturn(bt);
         when(response.getStatusCode()).thenReturn(201);

         Assert.assertEquals("Patch command must communicate success on 201", Boolean.TRUE,
                 patchCommand.handleResponse(response));
      }

      @Test(expected = DatastoreOperationException.class)
      public void shouldThrowExeptionOnUnauthorized() throws Exception {
         final String patchData = "Patch data";
         final int ttl = 30;
         final Patch patchCommand = new Patch(TimeUnit.MINUTES, patchData.getBytes(),
                 ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         when(response.getStatusCode()).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue());

         patchCommand.handleResponse(response);
      }
   }
}
