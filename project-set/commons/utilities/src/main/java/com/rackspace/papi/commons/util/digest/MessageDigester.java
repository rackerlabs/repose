package com.rackspace.papi.commons.util.digest;

import java.io.InputStream;

public interface MessageDigester {

   byte[] digestStream(InputStream stream);

   byte[] digestBytes(byte[] bytes);
}
