package com.rackspace.papi.commons.util.digest;

import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;

import java.io.InputStream;
import java.security.MessageDigest;

public abstract class AbstractMessageDigester implements MessageDigester {

   private final Pool<MessageDigest> messageDigestPool;

   public AbstractMessageDigester() {
      messageDigestPool = new GenericBlockingResourcePool<MessageDigest>(
              new MessageDigestConstructionStrategy(digestSpecName()));
   }

   protected abstract String digestSpecName();

   @Override
   public byte[] digestBytes(byte[] bytes) {
      return messageDigestPool.use(new ByteArrayResourceContext(bytes));
   }

   @Override
   public byte[] digestStream(final InputStream stream) {
      return messageDigestPool.use(new MessageDigestResourceContext(stream));
   }
}
