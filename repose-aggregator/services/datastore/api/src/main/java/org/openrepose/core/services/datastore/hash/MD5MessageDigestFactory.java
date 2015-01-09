package org.openrepose.core.services.datastore.hash;

public final class MD5MessageDigestFactory extends AbstractMessageDigestFactory {

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
