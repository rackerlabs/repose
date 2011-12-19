package com.rackspace.papi.service.datastore.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author zinic
 */
public abstract class AbstractMessageDigestFactory implements MessageDigestFactory {

   @Override
   public MessageDigest newMessageDigest() throws NoSuchAlgorithmException {
      return MessageDigest.getInstance(algorithmName());
   }
}
