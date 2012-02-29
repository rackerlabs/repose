package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashRingDatastore extends AbstractHashRingDatastore {

   private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);

   public HashRingDatastore(MutableClusterView clusterView, String datasetPrefix, Datastore localDatastore, MessageDigestFactory hashProvider, EncodingProvider encodingProvider) {
      super(clusterView, datasetPrefix, localDatastore, hashProvider, encodingProvider);
   }

   @Override
   protected void clusterMemberDamaged(InetSocketAddress member, MutableClusterView clusterView, RemoteConnectionException ex) {
      LOG.warn("Dropping cluster member: "
              + member.getAddress().toString()
              + ":" + member.getPort()
              + " - Reason: " + ex.getCause().getClass().getName() + ": " + ex.getCause().getMessage());

      clusterView.memberDropoped(member);
   }
}