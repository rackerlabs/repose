package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.api.client.ClientResponse;
import java.io.IOException;
import java.io.InputStream;

public class JerseyInputStream extends InputStream {
   private final ClientResponse response;
   private final InputStream source;
   
   public JerseyInputStream(ClientResponse reponse) {
      this.response = reponse;
      this.source = response.getEntityInputStream();
   }

   @Override
   public int read() throws IOException {
      if (source == null) {
         return -1;
      }
      
      return source.read();
   }
   
   @Override
   public int read(byte[] bytes) throws IOException {
      if (source == null) {
         return -1;
      }
      
      return source.read(bytes);
   }
   
   @Override
   public int read(byte[] bytes, int i, int i1) throws IOException {
      if (source == null) {
         return -1;
      }

      return source.read(bytes, i, i1);
   }

   @Override
   public long skip(long l) throws IOException {
      if (source == null) {
         return -1;
      }
      return source.skip(l);
   }

   @Override
   public int available() throws IOException {
      if (source == null) {
         return 0;
      }
      
      return source.available();
   }

   @Override
   public void close() throws IOException {
      if (source != null) {
         source.close();
      }
      
      response.close();
   }

   @Override
   public synchronized void mark(int i) {
      if (source != null) {
         source.mark(i);
      }
   }

   @Override
   public synchronized void reset() throws IOException {
      if (source != null) {
         source.reset();
      }
   }

   @Override
   public boolean markSupported() {
      if (source != null) {
         return source.markSupported();
      }
      
      return false;
   }
   
}
