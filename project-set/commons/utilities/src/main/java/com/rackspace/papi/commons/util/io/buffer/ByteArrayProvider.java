package com.rackspace.papi.commons.util.io.buffer;

/**
 *
 * @author zinic
 */
public interface ByteArrayProvider {

   byte[] allocate(int capacity);
}
