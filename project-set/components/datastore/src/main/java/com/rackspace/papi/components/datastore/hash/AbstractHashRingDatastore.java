package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;
import com.rackspace.papi.service.datastore.impl.AbstractHashedDatastore;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHashRingDatastore extends AbstractHashedDatastore {

   private static final Logger LOG = LoggerFactory.getLogger(AbstractHashRingDatastore.class);
   private final MutableClusterView clusterView;
   private final Datastore localDatastore;
   private RemoteCacheClient remoteCache;

   public AbstractHashRingDatastore(MutableClusterView clusterView, String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider, EncodingProvider encodingProvider) {
      super(datastorePrefix, encodingProvider, hashProvider);

      this.clusterView = clusterView;
      this.localDatastore = localDatastore;
   }

   protected abstract void clusterMemberDamaged(InetSocketAddress member, MutableClusterView clusterView, RemoteConnectionException ex);

   public void setRemoteCacheClient(RemoteCacheClient remoteCache) {
      this.remoteCache = remoteCache;
   }

   private InetSocketAddress getTarget(byte[] hashBytes) {
      final InetSocketAddress[] ringMembers = clusterView.members();

      if (ringMembers.length <= 0) {
         LOG.debug("No members to route to in datastore cluster. Returning local node.");

         return clusterView.localMember();
      }

      final BigInteger ringSliceSize = getHashProvider().largestDigestValue().divide(BigInteger.valueOf(ringMembers.length));
      final int memberAddress = new BigInteger(hashBytes).divide(ringSliceSize).abs().intValue();

      if (memberAddress > ringMembers.length) {
         throw new UnaddressableKeyException("Unable to address given key");
      }

      return ringMembers[memberAddress];
   }

   @Override
   protected StoredElement get(String name, byte[] id) throws DatastoreOperationException {
      boolean retry;

      do {
         retry = false;

         final InetSocketAddress target = getTarget(id);

         if (target != null && !target.equals(clusterView.localMember())) {
            LOG.debug(clusterView.localMember() + ":: Routing datastore get request for, \"" + name + "\" to: " + target.toString());

            try {
               return remoteCache.get(name, target);
            } catch (RemoteConnectionException rce) {
               clusterMemberDamaged(target, clusterView, rce);
               retry = true;
            }
         }
      } while (retry);

      return localDatastore.get(name);
   }

   @Override
   protected boolean remove(String name, byte[] id) throws DatastoreOperationException {
      boolean retry;

      do {
         retry = false;

         final InetSocketAddress target = getTarget(id);

         if (target != null && !target.equals(clusterView.localMember())) {
            LOG.debug(clusterView.localMember() + ":: Routing datastore delete request for, \"" + name + "\" to: " + target.toString());

            try {
               return remoteCache.delete(name, target);
            } catch (RemoteConnectionException rce) {
               clusterMemberDamaged(target, clusterView, rce);
               retry = true;
            }
         }
      } while (retry);

      return localDatastore.remove(name);
   }

   @Override
   protected void put(String name, byte[] id, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
      boolean retry;

      do {
         retry = false;

         final InetSocketAddress target = getTarget(id);

         if (target != null && !target.equals(clusterView.localMember())) {
            LOG.debug(clusterView.localMember() + ":: Routing datastore put request for, \"" + name + "\" to: " + target.toString());

            try {
               remoteCache.put(name, value, ttl, timeUnit, target);
               return;
            } catch (RemoteConnectionException rce) {
               clusterMemberDamaged(target, clusterView, rce);
               retry = true;
            }
         }
      } while (retry);

      localDatastore.put(name, value, ttl, timeUnit);
   }
}