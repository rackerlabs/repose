package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.components.datastore.hash.remote.RemoteCommandExecutor;
import com.rackspace.papi.components.datastore.hash.remote.RemoteConnectionException;
import com.rackspace.papi.components.datastore.hash.remote.command.Delete;
import com.rackspace.papi.components.datastore.hash.remote.command.Get;
import com.rackspace.papi.components.datastore.hash.remote.command.Put;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;
import com.rackspace.papi.service.datastore.impl.AbstractHashedDatastore;
import com.rackspace.papi.service.proxy.RequestProxyService;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashRingDatastore extends AbstractHashedDatastore {

   private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);
   private final MutableClusterView clusterView;
   private final Datastore localDatastore;
   private RemoteCommandExecutor remoteCommandExecutor;
   private RequestProxyService proxyService;

   public HashRingDatastore(RequestProxyService proxyService, MutableClusterView clusterView, String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider, EncodingProvider encodingProvider) {
      super(datastorePrefix, encodingProvider, hashProvider);

      this.clusterView = clusterView;
      this.localDatastore = localDatastore;
      this.proxyService = proxyService;
   }

   public void setRemoteCommandExecutor(RemoteCommandExecutor remoteCommandExecutor) {
      this.remoteCommandExecutor = remoteCommandExecutor;
   }

   public InetSocketAddress getTarget(byte[] hashBytes) {
      final InetSocketAddress[] ringMembers = clusterView.members();

      if (ringMembers.length <= 0) {
         LOG.info("No members to route to in datastore cluster... unable to route this datastore request.");

         return null;
      }

      final int memberAddress = new BigInteger(hashBytes).mod(BigInteger.valueOf(ringMembers.length)).abs().intValue();
      return ringMembers[memberAddress];
   }

   private boolean isRemoteTarget(InetSocketAddress target) throws DatastoreOperationException {
      try {
         return !clusterView.isLocal(target);
      } catch (SocketException se) {
         throw new DatastoreOperationException("Unable to identify local cluster target datastore", se);
      }
   }

   private Object performAction(String name, byte[] id, DatastoreAction action, RemoteBehavior initialBehavior) {
      boolean targetIsRemote = false;

      if (initialBehavior != RemoteBehavior.DISALLOW_FORWARDING) {
         RemoteBehavior remoteBehavior = clusterView.hasDamagedMembers() ? RemoteBehavior.DISALLOW_FORWARDING : initialBehavior;
         
         do {
            final InetSocketAddress target = getTarget(id);

            if (target != null && (targetIsRemote = isRemoteTarget(target))) {
               LOG.debug("Routing datastore " + action.toString() + " request for, \"" + name + "\" to: " + target.toString());

               try {
                  return action.performRemote(name, target, remoteBehavior);
               } catch (RemoteConnectionException rce) {
                  clusterView.memberDamaged(target, rce.getMessage());
                  remoteBehavior = RemoteBehavior.DISALLOW_FORWARDING;
               }
            }
         } while (targetIsRemote);
      } else {
         LOG.debug("Forwarding for " + action.toString() + " datastore action has been disabled by request.");
      }

      return action.performLocal(name);
   }

   @Override
   protected StoredElement get(String name, byte[] id) throws DatastoreOperationException {
      return get(name, id, RemoteBehavior.ALLOW_FORWARDING);
   }

   @Override
   protected boolean remove(String name, byte[] id) throws DatastoreOperationException {
      return remove(name, id, RemoteBehavior.ALLOW_FORWARDING);
   }

   @Override
   protected void put(String name, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit) throws DatastoreOperationException {
      put(name, id, value, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
   }

   public StoredElement get(String name, byte[] id, RemoteBehavior initialBehavior) throws DatastoreOperationException {
      return (StoredElement) performAction(name, id, new DatastoreAction() {

         @Override
         public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
            return remoteCommandExecutor.execute(new Get(name, target, remoteBehavior), remoteBehavior);
         }

         @Override
         public Object performLocal(String name) {
            return localDatastore.get(name);
         }

         @Override
         public String toString() {
            return "get";
         }
      }, initialBehavior);
   }

   public boolean remove(String name, byte[] id, RemoteBehavior initialBehavior) throws DatastoreOperationException {
      return (Boolean) performAction(name, id, new DatastoreAction() {

         @Override
         public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
            return remoteCommandExecutor.execute(new Delete(name, target), remoteBehavior);
         }

         @Override
         public Object performLocal(String name) {
            return localDatastore.remove(name);
         }

         @Override
         public String toString() {
            return "remove";
         }
      }, initialBehavior);
   }

   public void put(String name, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit, RemoteBehavior initialBehavior) throws DatastoreOperationException {
      performAction(name, id, new DatastoreAction() {

         @Override
         public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
            return remoteCommandExecutor.execute(new Put(timeUnit, value, ttl, name, target), remoteBehavior);
         }

         @Override
         public Object performLocal(String name) {
            localDatastore.put(name, value, ttl, timeUnit);

            return null;
         }

         @Override
         public String toString() {
            return "put";
         }
      }, initialBehavior);
   }
}