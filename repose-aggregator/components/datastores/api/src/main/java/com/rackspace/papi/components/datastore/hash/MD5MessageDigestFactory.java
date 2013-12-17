package com.rackspace.papi.components.datastore.hash;

public final class MD5MessageDigestFactory extends com.rackspace.papi.components.datastore.hash.AbstractMessageDigestFactory {

   private static final MessageDigestFactory INSTANCE = new MD5MessageDigestFactory();

   public static MessageDigestFactory getInstance() {
      return INSTANCE;
   }
   
   private static final String DIGEST_NAME = "MD5";

   @Override
   public String algorithmName() {
      return DIGEST_NAME;
   }

}
