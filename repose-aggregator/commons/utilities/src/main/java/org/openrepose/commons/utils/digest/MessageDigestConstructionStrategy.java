package org.openrepose.commons.utils.digest;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestConstructionStrategy extends BasePoolableObjectFactory<MessageDigest> {

   private final String digestSpecName;

   public MessageDigestConstructionStrategy(String digestSpecName) {
      this.digestSpecName = digestSpecName;
   }

   @Override
   public MessageDigest makeObject() throws Exception {
      try {
         return MessageDigest.getInstance(digestSpecName);
      } catch (NoSuchAlgorithmException nsae) {
         throw new ResourceConstructionException("Failed to locate digest object for digest " + digestSpecName, nsae);
      }
   }
}
