package com.rackspace.papi.components.datastore.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class AbstractMessageDigestFactory implements MessageDigestFactory {

   @Override
   public MessageDigest newMessageDigest() throws NoSuchAlgorithmException {
      return MessageDigest.getInstance(algorithmName());
   }
}
