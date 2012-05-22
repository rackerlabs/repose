package com.rackspace.papi.components.datastore.hash.remote.command;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.components.datastore.common.CacheRequest;
import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public class Get extends AbstractRemoteCommand<HttpGet> {

   public Get(String cacheObjectKey, InetSocketAddress remoteEndpoint, RemoteBehavior remoteBehavior) {
      super(cacheObjectKey, remoteEndpoint, remoteBehavior);
   }

   @Override
   protected HttpGet newHttpRequestBase() {
      final String targetUrl = CacheRequest.urlFor(getRemoteEndpoint(), getCacheObjectKey());

      return new HttpGet(targetUrl);
   }

   @Override
   public Object handleResponse(HttpResponse httpResponse) throws IOException, DatastoreOperationException {
      final int statusCode = httpResponse.getStatusLine().getStatusCode();

      if (statusCode == HttpStatusCode.OK.intValue()) {
         final InputStream internalStreamReference = httpResponse.getEntity().getContent();

         return new StoredElementImpl(getCacheObjectKey(), RawInputStreamReader.instance().readFully(internalStreamReference));
      } else if (statusCode != HttpStatusCode.NOT_FOUND.intValue()) {
         throw new DatastoreOperationException("Remote request failed with: " + statusCode);
      }
      
      return new StoredElementImpl(getCacheObjectKey(), null);
   }
}
