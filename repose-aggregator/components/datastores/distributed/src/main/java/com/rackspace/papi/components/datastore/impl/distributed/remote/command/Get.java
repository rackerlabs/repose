package com.rackspace.papi.components.datastore.impl.distributed.remote.command;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;

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

          try {
              return ObjectSerializer.instance().readObject(RawInputStreamReader.instance().readFully(internalStreamReference));
          } catch (ClassNotFoundException cnfe) {
              throw new DatastoreOperationException("Unable to marshall a java object from stored element contents. Reason: " + cnfe.getMessage(), cnfe);
          }
      } else if (statusCode == HttpStatusCode.NOT_FOUND.intValue()) {
         return null;
      }

      throw new DatastoreOperationException("Remote request failed with: " + statusCode);
   }
}
