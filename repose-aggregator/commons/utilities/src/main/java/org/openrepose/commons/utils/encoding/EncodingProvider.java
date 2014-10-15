package org.openrepose.commons.utils.encoding;

public interface EncodingProvider {

    String encode(byte[] hash);

    byte[] decode(String hash);
}
