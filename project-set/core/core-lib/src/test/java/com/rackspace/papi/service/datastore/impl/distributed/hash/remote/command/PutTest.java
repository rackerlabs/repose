/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.hash.remote.command;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.distributed.common.CacheRequest;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class PutTest {

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectPutUrl() throws UnknownHostException {
         //final Put putCommand = new Put("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
         final String putData = "Put data";
         final int ttl = 30;
         final String key = "someKey";
         final Put putCommand = new Put(TimeUnit.MINUTES, putData.getBytes(), ttl, key, new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         assertEquals("Get command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + key, putCommand.getUrl());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOnSuccess() throws Exception {
         final String putData = "Put data";
         final int ttl = 30;
         final Put putCommand = new Put(TimeUnit.MINUTES, putData.getBytes(), ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         // RemoteBehavior.ALLOW_FORWARDING
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         final String responseData = "Response Data";

         ByteArrayInputStream bt = new ByteArrayInputStream(responseData.getBytes("UTF-8"));

         when(response.getData()).thenReturn(bt);
         when(response.getStatusCode()).thenReturn(202);

         assertEquals("Put command must communicate success on 202", Boolean.TRUE, putCommand.handleResponse(response));
      }

      
      @Test(expected = DatastoreOperationException.class)
      public void shouldThrowExeptionOnUnauthorized() throws Exception {
         final String putData = "Put data";
         final int ttl = 30;
         final Put putCommand = new Put(TimeUnit.MINUTES, putData.getBytes(), ttl, "somekey", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         when(response.getStatusCode()).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue());

         putCommand.handleResponse(response);
      }
   }
}