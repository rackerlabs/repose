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
public class GetTest {

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectDeletionUrl() throws UnknownHostException {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         assertEquals("Get command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", getCommand.getUrl());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOnSuccess() throws Exception {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         // RemoteBehavior.ALLOW_FORWARDING
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         final String responseData = "Response Data";

         ByteArrayInputStream bt = new ByteArrayInputStream(responseData.getBytes("UTF-8"));

         when(response.getData()).thenReturn(bt);
         when(response.getStatusCode()).thenReturn(200);

         assertTrue("Get command must retrieve new StoredElement ", getCommand.handleResponse(response) instanceof StoredElement);
//         assertEquals("Get command must communicate success on 200", Boolean.TRUE, getCommand.handleResponse(response));
      }

      @Test(expected = DatastoreOperationException.class)
      public void shouldThrowExeptionOnUnauthorized() throws Exception {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         when(response.getStatusCode()).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue());
         
         getCommand.handleResponse(response);
      }
   }
}