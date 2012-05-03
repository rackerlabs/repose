package com.rackspace.papi.components.datastore.hash.remote.command;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.components.datastore.common.CacheRequest;
import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;

/**
 *
 * @author zinic
 */
public class Put extends AbstractRemoteCommand<HttpPut> {

   private final TimeUnit timeUnit;
   private final byte[] value;
   private final int ttl;

   public Put(TimeUnit timeUnit, byte[] value, int ttl, String cacheObjectKey, InetSocketAddress remoteEndpoint, RemoteBehavior remoteBehavior) {
      super(cacheObjectKey, remoteEndpoint, remoteBehavior);
      this.timeUnit = timeUnit;
      this.value = value;
      this.ttl = ttl;
   }

   @Override
   protected HttpPut newHttpRequestBase() {
      final String targetUrl = CacheRequest.urlFor(getRemoteEndpoint(), getCacheObjectKey());

      return new HttpPut(targetUrl);
   }

   @Override
   protected void prepareRequest(HttpPut httpRequestBase) {
      httpRequestBase.setEntity(new ByteArrayEntity(value));
      httpRequestBase.addHeader(ExtendedHttpHeader.X_TTL.toString(), String.valueOf(TimeUnit.SECONDS.convert(ttl, timeUnit)));
   }

   @Override
   public Object handleResponse(HttpResponse httpResponse) throws IOException, DatastoreOperationException {
      if (httpResponse.getStatusLine().getStatusCode() != HttpStatusCode.ACCEPTED.intValue()) {
         throw new DatastoreOperationException("Remote request failed with: " + httpResponse.getStatusLine().getStatusCode());
      }
      
      return Boolean.TRUE;
   }
}
