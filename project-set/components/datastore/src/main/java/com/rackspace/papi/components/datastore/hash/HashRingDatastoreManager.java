package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;

public class HashRingDatastoreManager implements DatastoreManager {

   public static final String DATASTORE_MANAGER_NAME = "distributed/hash-ring";
   private final String HOST_KEY = "temp-host-key";
   private final HashedDatastore datastore;
   private boolean available;

   public HashRingDatastoreManager(String hostKey, EncodingProvider encodingProvider, MessageDigestFactory hashProvider, MutableClusterView clusterView, Datastore localDatastore) {
      final HashRingDatastore newHashRingDatastore = new HashRingDatastore(clusterView, hostKey, localDatastore, hashProvider, encodingProvider);
      newHashRingDatastore.setRemoteCacheClient(newRemoteHttpCacheClient(HOST_KEY, 300, 5000));

      datastore = newHashRingDatastore;
      available = true;
   }

   private RemoteCacheClient newRemoteHttpCacheClient(String hostKey, int connectionTimeout, int socketTimeout) {
      final RemoteHttpCacheClientImpl newRemoteHttpCacheClient = new RemoteHttpCacheClientImpl(connectionTimeout, socketTimeout);
      newRemoteHttpCacheClient.setHostKey(hostKey);

      return newRemoteHttpCacheClient;
   }

   @Override
   public Datastore getDatastore() {
      return datastore;
   }
   
   @Override
   public boolean isAvailable() {
      return available;
   }

   @Override
   public void destroy() {
      available = false;
   }

   @Override
   public boolean isDistributed() {
      return true;
   }
}