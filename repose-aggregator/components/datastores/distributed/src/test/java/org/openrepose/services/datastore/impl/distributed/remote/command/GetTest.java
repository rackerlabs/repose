package org.openrepose.services.datastore.impl.distributed.remote.command;

import org.openrepose.commons.utils.http.HttpStatusCode;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.services.datastore.DatastoreOperationException;
import org.openrepose.services.datastore.impl.distributed.CacheRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class GetTest {

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectDeletionUrl() throws UnknownHostException {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         Assert.assertEquals("Get command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", getCommand.getUrl());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnTrueOnSuccess() throws Exception {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000));

         // RemoteBehavior.ALLOW_FORWARDING
         final ServiceClientResponse response = mock(ServiceClientResponse.class);
         final String responseData = "Response Data";

         ByteArrayInputStream bt = new ByteArrayInputStream(ObjectSerializer.instance().writeObject(responseData));

         when(response.getData()).thenReturn(bt);
         when(response.getStatusCode()).thenReturn(200);

         assertThat((String)getCommand.handleResponse(response), equalTo(responseData));
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
