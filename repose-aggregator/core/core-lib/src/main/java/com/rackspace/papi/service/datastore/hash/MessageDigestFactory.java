package com.rackspace.papi.service.datastore.hash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author zinic
 */
public interface MessageDigestFactory {

   String algorithmName();
   
   MessageDigest newMessageDigest() throws NoSuchAlgorithmException;

   BigInteger largestDigestValue();
}
