package com.rackspace.papi.commons.util.encoding;

public interface EncodingProvider {

    String encode(byte[] hash);

    byte[] decode(String hash);
}
