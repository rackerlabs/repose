package com.rackspace.papi.service.datastore.encoding;

public final class UUIDEncodingProvider implements EncodingProvider {

   private static final UUIDEncodingProvider INSTANCE = new UUIDEncodingProvider();

   public static EncodingProvider getInstance() {
      return INSTANCE;
   }

   private UUIDEncodingProvider() {
   }

   @Override
   public byte[] decode(String hash) {
      return UUIDHelper.stringToUUIDBytes(hash);
   }

   @Override
   public String encode(byte[] hash) {
      return UUIDHelper.bytesToUUID(hash).toString();
   }
}
