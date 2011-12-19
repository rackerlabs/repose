package com.rackspace.papi.service.datastore.hash;

import java.math.BigInteger;

/**
 *
 * @author zinic
 */
public final class MD5MessageDigestFactory extends AbstractMessageDigestFactory {

   private static final MessageDigestFactory INSTANCE = new MD5MessageDigestFactory();

   public static MessageDigestFactory getInstance() {
      return INSTANCE;
   }
   
   private static final String DIGEST_NAME = "MD5";
   private static final BigInteger MAX_VALUE = new BigInteger(
           new byte[]{
              0x7F, 0x7F, 0x7F, 0x7F,
              0x7F, 0x7F, 0x7F, 0x7F,
              0x7F, 0x7F, 0x7F, 0x7F,
              0x7F, 0x7F, 0x7F, 0x7F,});
 
   @Override
   public String algorithmName() {
      return DIGEST_NAME;
   }

   @Override
   public BigInteger largestDigestValue() {
      return MAX_VALUE;
   }
}
