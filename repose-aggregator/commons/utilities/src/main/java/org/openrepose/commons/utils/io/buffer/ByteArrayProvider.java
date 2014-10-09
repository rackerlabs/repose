package org.openrepose.commons.utils.io.buffer;

/**
 *
 * @author zinic
 */
public interface ByteArrayProvider {

   byte[] allocate(int capacity);
}
