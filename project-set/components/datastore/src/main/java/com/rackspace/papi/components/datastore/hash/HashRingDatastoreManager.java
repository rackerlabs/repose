package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashProvider;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.impl.AbstractMapDatastoreManager;

import java.util.HashMap;

public class HashRingDatastoreManager extends AbstractMapDatastoreManager<HashedDatastore> {

   public static final String DATASTORE_MANAGER_NAME = "distributed/hash-ring";
   private final Datastore localDatastore;
   private final MutableClusterView clusterView;
   private final RemoteCacheClient datastoreClientRemote, datastoreServerRemote;
   private final EncodingProvider encodingProvider;
   private final HashProvider hashProvider;

   public HashRingDatastoreManager(String hostKey, EncodingProvider encodingProvider, HashProvider hashProvider, MutableClusterView clusterView, Datastore localDatastore) {
      super(new HashMap<String, HashedDatastore>());

      this.localDatastore = localDatastore;
      this.clusterView = clusterView;
      this.encodingProvider = encodingProvider;
      this.hashProvider = hashProvider;

      RemoteHttpCacheClientImpl newClient = new RemoteHttpCacheClientImpl();
      newClient.setHostKey(hostKey);

      datastoreServerRemote = newClient;

      newClient = new RemoteHttpCacheClientImpl(1000, 10000);
      newClient.setHostKey(hostKey);

      datastoreClientRemote = newClient;
   }

   @Override
   protected HashedDatastore newDatastore(String key) {
      final HashRingDatastore datastore = new HashRingDatastore(clusterView, key, localDatastore, hashProvider, encodingProvider);
      datastore.setRemoteCacheClient(datastoreClientRemote);

      return datastore;
   }

   public HashedDatastore newDatastoreServer(String key) {
      final HashRingDatastore datastore = new HashRingDatastore(clusterView, key, localDatastore, hashProvider, encodingProvider);
      datastore.setRemoteCacheClient(datastoreServerRemote);

      return datastore;
   }

   @Override
   public boolean isDistributed() {
      return true;
   }
}