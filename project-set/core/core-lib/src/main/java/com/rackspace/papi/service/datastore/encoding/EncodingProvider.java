package com.rackspace.papi.service.datastore.encoding;

public interface EncodingProvider {

    String encode(byte[] hash);

    byte[] decode(String hash);
}
