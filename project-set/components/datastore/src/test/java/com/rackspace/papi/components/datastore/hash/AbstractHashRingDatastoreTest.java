package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class AbstractHashRingDatastoreTest {

   public static class WhenAddressingTargets {

      protected InetAddress[] addresses;
      protected MutableClusterView clusterVew, emptyClusterView;
      protected Datastore mockedDatastore;
      protected TestingHashRingDatastore datastore;

      @Before
      public void standUp() throws Exception {
         addresses = new InetAddress[]{
            InetAddress.getByAddress(new byte[]{10, 1, 1, 11}),
            InetAddress.getByAddress(new byte[]{10, 1, 1, 12}),
            InetAddress.getByAddress(new byte[]{10, 1, 1, 13}),
            InetAddress.getByAddress(new byte[]{10, 1, 1, 14})
         };

         clusterVew = mock(MutableClusterView.class);
         emptyClusterView = mock(MutableClusterView.class);
         
         when(emptyClusterView.localMember()).thenReturn(new InetSocketAddress(addresses[0], 2200));
         when(emptyClusterView.members()).thenReturn(new InetSocketAddress[0]);

         when(clusterVew.members()).thenReturn(new InetSocketAddress[]{
                    new InetSocketAddress(addresses[0], 2200),
                    new InetSocketAddress(addresses[1], 2200),
                    new InetSocketAddress(addresses[2], 2200),
                    new InetSocketAddress(addresses[3], 2200),});

         mockedDatastore = mock(Datastore.class);
      }

      @Test
      public void shouldSelectCorrectTarget() throws Exception {
         datastore = new TestingHashRingDatastore(clusterVew, "", mockedDatastore, MD5MessageDigestFactory.getInstance(), UUIDEncodingProvider.getInstance());
         
         final byte[] id = new byte[] {1};

         for (byte i=0; i<127; i++) {
            id[0] = i;
            
            assertEquals("Addressing must select correct target", addresses[i % 4], datastore.getTarget(id).getAddress());
         }
      }

      @Test
      public void shouldUseLocalTarget() throws Exception {
         datastore = new TestingHashRingDatastore(emptyClusterView, "", mockedDatastore, MD5MessageDigestFactory.getInstance(), UUIDEncodingProvider.getInstance());
         
         final byte[] id = new byte[] {1};

         assertEquals("If cluster is empty, datastore must use local member", addresses[0], datastore.getTarget(id).getAddress());
      }
   }

   @Ignore
   public static class TestingHashRingDatastore extends AbstractHashRingDatastore {

      public TestingHashRingDatastore(MutableClusterView clusterView, String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider, EncodingProvider encodingProvider) {
         super(clusterView, datastorePrefix, localDatastore, hashProvider, encodingProvider);
      }

      @Override
      protected void clusterMemberDamaged(InetSocketAddress member, MutableClusterView clusterView, RemoteConnectionException ex) {
         throw new UnsupportedOperationException("Not supported yet.");
      }
   }
}
