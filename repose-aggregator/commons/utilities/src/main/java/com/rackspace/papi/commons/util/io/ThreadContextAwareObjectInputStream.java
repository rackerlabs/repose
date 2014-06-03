package com.rackspace.papi.commons.util.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ThreadContextAwareObjectInputStream extends ObjectInputStream {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadContextAwareObjectInputStream.class);
   public ThreadContextAwareObjectInputStream(InputStream in) throws IOException {
      super(in);
   }

   @Override
   protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      final ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();

      try {
         return threadContextClassLoader.loadClass(desc.getName());
      } catch (ClassNotFoundException ignored) {
          LOG.trace("Couldn't load class {}.", desc.getName(), ignored);
      }

      return super.resolveClass(desc);
   }
}
