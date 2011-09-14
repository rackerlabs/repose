package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.components.datastore.hash.UnaddressableKeyException;
import com.rackspace.papi.components.datastore.hash.HashRingDatastore;
import com.rackspace.papi.service.datastore.cluster.ClusterView;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class HashRingDatastoreTest {
    public static class WhenAddressingKeys {
        
        private MutableClusterView clusterView;
        private HashRingDatastore datastore;
        
        @Before
        public void standUp() throws Exception {
            clusterView = new ThreadSafeClusterView();
            
            clusterView.updateLocal(new InetSocketAddress(InetAddress.getLocalHost(), 5000));
            
            clusterView.updateMembers(new InetSocketAddress[] {
                new InetSocketAddress(InetAddress.getLocalHost(), 5000),
                new InetSocketAddress(InetAddress.getLocalHost(), 5001),
                new InetSocketAddress(InetAddress.getLocalHost(), 5002)
            });
            
            datastore = new HashRingDatastore("TEST", clusterView, null) {

                @Override
                protected String hashBytesToSTring(byte[] hash) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                protected byte[] stringToHashBytes(String hash) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
                
                @Override
                protected BigInteger maxValue() {
                    return BigInteger.valueOf(Integer.MAX_VALUE);
                }

                @Override
                protected byte[] hash(String key) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };    
        }
        
        @Test
        public void shouldReturnLocalhostWhenNoClusterMemebersArePresent() {
            clusterView.updateMembers(new InetSocketAddress[0]);
            
            final InetSocketAddress expectedAddress = clusterView.local();
            
            assertEquals(datastore.getTarget(BigInteger.valueOf((long) Integer.MAX_VALUE / 2).toByteArray()), expectedAddress);
        }
        
        @Test (expected=UnaddressableKeyException.class)
        public void shouldRejectUnaddressableKeys() {
            datastore.getTarget(BigInteger.valueOf((long) Integer.MAX_VALUE * 2).toByteArray());
        }
        
        @Test
        public void shouldAddressValidKeys() throws Exception {
            final InetSocketAddress expectedAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5001);
            
            assertEquals(datastore.getTarget(BigInteger.valueOf((long) Integer.MAX_VALUE / 2).toByteArray()), expectedAddress);
        }
    }
}
