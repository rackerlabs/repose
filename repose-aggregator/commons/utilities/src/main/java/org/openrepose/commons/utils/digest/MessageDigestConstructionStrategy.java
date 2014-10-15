package org.openrepose.commons.utils.digest;

import org.openrepose.commons.utils.pooling.ConstructionStrategy;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestConstructionStrategy implements ConstructionStrategy<MessageDigest> {

   private final String digestSpecName;

   public MessageDigestConstructionStrategy(String digestSpecName) {
      this.digestSpecName = digestSpecName;
   }

   @Override
   public MessageDigest construct() throws ResourceConstructionException {
      try {
         return MessageDigest.getInstance(digestSpecName);
      } catch (NoSuchAlgorithmException nsae) {
         throw new ResourceConstructionException("Failed to locate digest object for digest " + digestSpecName, nsae);
      }
   }
}
