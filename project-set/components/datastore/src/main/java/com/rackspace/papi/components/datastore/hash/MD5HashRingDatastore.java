package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.cluster.ClusterView;
import com.rackspace.papi.service.datastore.Datastore;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5HashRingDatastore extends HashRingDatastore {

    private static final byte[] MAX_VALUE = new byte[]{
        0x7F, 0x7F, 0x7F, 0x7F,
        0x7F, 0x7F, 0x7F, 0x7F,
        0x7F, 0x7F, 0x7F, 0x7F,
        0x7F, 0x7F, 0x7F, 0x7F,};
    
    private final MessageDigest digestAlgorithm;

    public MD5HashRingDatastore(String datastorePrefix, ClusterView clusterView, Datastore localDatastore) throws NoSuchAlgorithmException {
        super(datastorePrefix, clusterView, localDatastore);
        
        digestAlgorithm = MessageDigest.getInstance("MD5");
    }

    @Override
    protected synchronized byte[] hash(String key) {
        return digestAlgorithm.digest(key.getBytes());
    }

    @Override
    protected BigInteger maxValue() {
        return new BigInteger(MAX_VALUE);
    }
}