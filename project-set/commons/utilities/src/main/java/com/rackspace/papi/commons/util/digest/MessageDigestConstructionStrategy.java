package com.rackspace.papi.commons.util.digest;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;

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
