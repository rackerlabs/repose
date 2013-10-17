package com.rackspace.papi.commons.util.io.buffer;

import java.io.IOException;

public interface ByteBuffer extends Cloneable {

   byte get() throws IOException;

   int get(byte[] b) throws IOException;

   int get(byte[] b, int off, int len) throws IOException;

   void put(byte b) throws IOException;

   int put(byte[] b) throws IOException;

   int put(byte[] b, int off, int len) throws IOException;

   int skip(int bytes);

   int remaining();

   int available();

   void clear();

   ByteBuffer copy();
}