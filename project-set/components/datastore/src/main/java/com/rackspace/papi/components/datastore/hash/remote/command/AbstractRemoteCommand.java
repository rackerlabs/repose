package com.rackspace.papi.components.datastore.hash.remote.command;

import com.rackspace.papi.components.datastore.common.DatastoreHeader;
import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.components.datastore.hash.remote.RemoteCommand;
import org.apache.http.client.methods.HttpRequestBase;

import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public abstract class AbstractRemoteCommand<T extends HttpRequestBase> implements RemoteCommand {

   private final InetSocketAddress remoteEndpoint;
   private final RemoteBehavior remoteBehavior;
   private final String cacheObjectKey;
   private String hostKey;

   public AbstractRemoteCommand(String cacheObjectKey, InetSocketAddress remoteEndpoint, RemoteBehavior remoteBehavior) {
      this.cacheObjectKey = cacheObjectKey;
      this.remoteEndpoint = remoteEndpoint;
      this.remoteBehavior = remoteBehavior;
   }

   protected abstract T newHttpRequestBase();

   protected void prepareRequest(T httpRequestBase) {
   }

   @Override
   public final T buildRequest() {
      final T requestBase = newHttpRequestBase();

      setDefaultHeaders(requestBase, remoteBehavior);
      prepareRequest(requestBase);

      return requestBase;
   }

   private void setDefaultHeaders(HttpRequestBase httpMessage, RemoteBehavior remoteBehavior) {
      httpMessage.addHeader(DatastoreHeader.HOST_KEY.toString(), hostKey);
      httpMessage.addHeader(DatastoreHeader.REMOTE_BEHAVIOR.toString(), remoteBehavior.name());
   }

   protected InetSocketAddress getRemoteEndpoint() {
      return remoteEndpoint;
   }

   protected String getCacheObjectKey() {
      return cacheObjectKey;
   }

   @Override
   public void setHostKey(String hostKey) {
      this.hostKey = hostKey;
   }
}
