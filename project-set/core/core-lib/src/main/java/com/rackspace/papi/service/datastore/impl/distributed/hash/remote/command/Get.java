package com.rackspace.papi.service.datastore.impl.distributed.hash.remote.command;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.service.datastore.impl.distributed.common.RemoteBehavior;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public class Get extends AbstractRemoteCommand {

   public Get(String cacheObjectKey, InetSocketAddress remoteEndpoint) {
      super(cacheObjectKey, remoteEndpoint);
   }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.get(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior));
    }
    
   @Override
   public Object handleResponse(ServiceClientResponse response) throws IOException {
      final int statusCode = response.getStatusCode();

      if (statusCode == HttpStatusCode.OK.intValue()) {
         final InputStream internalStreamReference = response.getData();

         return new StoredElementImpl(getCacheObjectKey(), RawInputStreamReader.instance().readFully(internalStreamReference));
      } else if (statusCode != HttpStatusCode.NOT_FOUND.intValue()) {
         throw new DatastoreOperationException("Remote request failed with: " + statusCode);
      }
      
      return new StoredElementImpl(getCacheObjectKey(), null);
   }
}
