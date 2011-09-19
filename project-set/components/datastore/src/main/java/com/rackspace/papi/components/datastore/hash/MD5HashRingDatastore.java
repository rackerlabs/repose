package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class MD5HashRingDatastore extends HashRingDatastore {

    private static final byte[] MAX_VALUE = new byte[]{
        0x7F, 0x7F, 0x7F, 0x7F,
        0x7F, 0x7F, 0x7F, 0x7F,
        0x7F, 0x7F, 0x7F, 0x7F,
        0x7F, 0x7F, 0x7F, 0x7F,};
    private final MessageDigest digestAlgorithm;

    public MD5HashRingDatastore(String datastorePrefix, MutableClusterView clusterView, Datastore localDatastore) throws NoSuchAlgorithmException {
        super(datastorePrefix, clusterView, localDatastore);

        digestAlgorithm = MessageDigest.getInstance("MD5");
    }

    @Override
    protected String hashBytesToSTring(byte[] hash) {
        return UUID.nameUUIDFromBytes(hash).toString();
    }

    @Override
    protected byte[] stringToHashBytes(String hash) {
        final UUID uuid = UUID.fromString(hash);
        
        final long msb = uuid.getMostSignificantBits();
        final long lsb = uuid.getLeastSignificantBits();
        final byte[] buffer = new byte[16];

        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> 8 * (7 - i));
        }
        
        for (int i = 8; i < 16; i++) {
            buffer[i] = (byte) (lsb >>> 8 * (7 - i));
        }

        return buffer;
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