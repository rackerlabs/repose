package com.rackspace.papi.components.datastore.hash.remote.command;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.components.datastore.common.CacheRequest;
import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public class Delete extends AbstractRemoteCommand<HttpDelete> {

   public Delete(String cacheObjectKey, InetSocketAddress remoteEndpoint, RemoteBehavior remoteBehavior) {
      super(cacheObjectKey, remoteEndpoint, remoteBehavior);
   }

   @Override
   protected HttpDelete newHttpRequestBase() {
      final String targetUrl = CacheRequest.urlFor(getRemoteEndpoint(), getCacheObjectKey());

      return new HttpDelete(targetUrl);
   }

   @Override
   public Object handleResponse(HttpResponse httpResponse) throws IOException, DatastoreOperationException {
      return Boolean.valueOf(httpResponse.getStatusLine().getStatusCode() == HttpStatusCode.ACCEPTED.intValue());
   }
}
