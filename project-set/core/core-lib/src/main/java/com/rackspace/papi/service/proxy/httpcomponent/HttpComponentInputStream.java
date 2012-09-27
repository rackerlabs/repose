package com.rackspace.papi.service.proxy.httpcomponent;

import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

public class HttpComponentInputStream extends InputStream {
   private final HttpEntity entity;
   private final InputStream source;
   
   public HttpComponentInputStream(HttpEntity entity) throws IOException {
      if (entity == null) {
         throw new IllegalArgumentException("Entity cannot be null");
      }
      this.entity = entity;
      source = entity.getContent();
   }
   
   @Override
   public void close() throws IOException {
      if (entity != null) {
         EntityUtils.consume(entity);
      }
      
      if (source != null) {
          source.close();
      }
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
