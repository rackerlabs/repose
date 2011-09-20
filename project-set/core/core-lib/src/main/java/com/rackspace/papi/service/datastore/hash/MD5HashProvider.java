package com.rackspace.papi.service.datastore.hash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5HashProvider implements HashProvider {

    private static final BigInteger MAX_VALUE = new BigInteger(
            new byte[]{
                0x7F, 0x7F, 0x7F, 0x7F,
                0x7F, 0x7F, 0x7F, 0x7F,
                0x7F, 0x7F, 0x7F, 0x7F,
                0x7F, 0x7F, 0x7F, 0x7F,});
    
    private final MessageDigest digestAlgorithm;

    public MD5HashProvider() throws NoSuchAlgorithmException {
        digestAlgorithm = MessageDigest.getInstance("MD5");
    }

    @Override
    public synchronized byte[] hash(String key) {
        return digestAlgorithm.digest(key.getBytes());
    }

    @Override
    public BigInteger maxValue() {
        return MAX_VALUE;
    }
}
