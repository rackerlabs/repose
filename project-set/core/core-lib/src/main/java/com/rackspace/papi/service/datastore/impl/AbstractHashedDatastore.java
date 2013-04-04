package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractHashedDatastore implements Datastore {

    private final EncodingProvider encodingProvider;
    private final MessageDigestFactory hashProvider;
    private final String datasetPrefix;
    private static final int DEFAULT_TTL = 5;

    public AbstractHashedDatastore(String datasetPrefix, EncodingProvider encodingProvider,
            MessageDigestFactory digestProvider) {
        this.encodingProvider = encodingProvider;
        this.hashProvider = digestProvider;
        this.datasetPrefix = datasetPrefix;
    }

    public String getDatasetPrefix() {
        return datasetPrefix;
    }

    public EncodingProvider getEncodingProvider() {
        return encodingProvider;
    }

    private byte[] getHash(String key) {
        final byte[] stringBytes = (datasetPrefix + key).getBytes(CharacterSets.UTF_8);

        try {
            return hashProvider.newMessageDigest().digest(stringBytes);
        } catch (NoSuchAlgorithmException algorithmException) {
            throw new DatastoreOperationException("Failed to hash key. Reason: " + algorithmException.getMessage(),
                                                  algorithmException);
        }
    }

    @Override
    public final StoredElement get(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return get(encodingProvider.encode(keyHash), keyHash);
    }

    @Override
    public final boolean remove(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return remove(encodingProvider.encode(keyHash), keyHash);
    }

    @Override
    public final void put(String key, byte[] value) throws DatastoreOperationException {
        put(key, value, DEFAULT_TTL, TimeUnit.MINUTES);
    }

    @Override
    public final void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        put(encodingProvider.encode(keyHash), keyHash, value, ttl, timeUnit);
    }

    @Override
    public void removeAllCacheData() {
        removeAllCachedData();
    }

    protected abstract StoredElement get(String name, byte[] id) throws DatastoreOperationException;

    protected abstract boolean remove(String name, byte[] id) throws DatastoreOperationException;

    protected abstract void put(String name, byte[] id, byte[] value, int ttl, TimeUnit timeUnit)
            throws DatastoreOperationException;

    protected abstract void removeAllCachedData();
}
