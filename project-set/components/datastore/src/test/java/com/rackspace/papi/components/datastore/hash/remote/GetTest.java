package com.rackspace.papi.components.datastore.hash.remote;

import com.rackspace.papi.components.datastore.common.CacheRequest;
import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.components.datastore.hash.remote.command.Delete;
import com.rackspace.papi.components.datastore.hash.remote.command.Get;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.Ignore;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class GetTest {

   public static class WhenCreatingHttpRequestBase {

      @Test
      public void shouldTargetCorrectGetUrl() throws UnknownHostException {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         assertEquals("Get command must target expected URL", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + "object-key", getCommand.buildRequest().getURI().toString());
      }
   }

   public static class WhenProcessingResponse {

      @Test
      public void shouldReturnPopulatedCacheObjectOn200() throws Exception {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         final HttpEntity mockedEntity = mock(HttpEntity.class);
         when(mockedEntity.getContent()).thenReturn(new ByteArrayInputStream(new byte[1]));

         final HttpResponse mockedResponse = mock(HttpResponse.class);
         when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
         when(mockedResponse.getEntity()).thenReturn(mockedEntity);

         assertFalse("Successful get must return a populated, non-null cache object", ((StoredElement) getCommand.handleResponse(mockedResponse)).elementIsNull());
      }

      @Test
      public void shouldReturnNullCacheObjectOn404() throws Exception {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         final HttpResponse mockedResponse = mock(HttpResponse.class);
         when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "Not Found"));

         assertTrue("Getting a cache object that's not found must return a populated, null cache object", ((StoredElement) getCommand.handleResponse(mockedResponse)).elementIsNull());
      }

      @Test(expected = DatastoreOperationException.class)
      public void shouldCommunicateFailureAsException() throws Exception {
         final Get getCommand = new Get("object-key", new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000), RemoteBehavior.ALLOW_FORWARDING);

         final HttpResponse mockedResponse = mock(HttpResponse.class);
         when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 500, "Lewz"));
         
         getCommand.handleResponse(mockedResponse);
      }
   }
}
