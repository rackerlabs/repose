package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractHashedDatastore implements HashedDatastore {

   private final EncodingProvider encodingProvider;
   private final MessageDigestFactory hashProvider;
   private final String datasetPrefix;

   public AbstractHashedDatastore(String datasetPrefix, EncodingProvider encodingProvider, MessageDigestFactory digestProvider) {
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

   public MessageDigestFactory getHashProvider() {
      return hashProvider;
   }

   private byte[] getHash(String key) {
      final byte[] stringBytes = (datasetPrefix + key).getBytes();

      try {
         return hashProvider.newMessageDigest().digest(stringBytes);
      } catch(NoSuchAlgorithmException algorithmException) {
         throw new DatastoreOperationException("Failed to hash key. Reason: " + algorithmException.getMessage(), algorithmException);
      }
   }

   @Override
   public StoredElement get(String key) throws DatastoreOperationException {
      final byte[] keyHash = getHash(key);

      return get(encodingProvider.encode(keyHash), keyHash);
   }

   @Override
   public boolean remove(String key) throws DatastoreOperationException {
      final byte[] keyHash = getHash(key);

      return remove(encodingProvider.encode(keyHash), keyHash);
   }

   @Override
   public void put(String key, byte[] value) throws DatastoreOperationException {
      put(key, value, 5, TimeUnit.MINUTES);
   }

   @Override
   public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
      final byte[] keyHash = getHash(key);

      put(encodingProvider.encode(keyHash), keyHash, value, ttl, timeUnit);
   }

   @Override
   public StoredElement getByHash(String encodedHashString) throws DatastoreOperationException {
      return get(encodedHashString, encodingProvider.decode(encodedHashString));
   }

   @Override
   public boolean removeByHash(String encodedHashString) throws DatastoreOperationException {
      return remove(encodedHashString, encodingProvider.decode(encodedHashString));
   }

   @Override
   public void putByHash(String encodedHashString, byte[] value) {
      put(encodedHashString, encodingProvider.decode(encodedHashString), value, 3, TimeUnit.MINUTES);
   }

   @Override
   public void putByHash(String encodedHashString, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
      put(encodedHashString, encodingProvider.decode(encodedHashString), value, ttl, timeUnit);
   }

   protected abstract StoredElement get(String name, byte[] id) throws DatastoreOperationException;

   protected abstract boolean remove(String name, byte[] id) throws DatastoreOperationException;

   protected abstract void put(String name, byte[] id, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;
}
