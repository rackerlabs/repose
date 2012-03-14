package com.rackspace.papi.commons.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ThreadContextAwareObjectInputStream extends ObjectInputStream {

   public ThreadContextAwareObjectInputStream(InputStream in) throws IOException {
      super(in);
   }

   @Override
   protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      final ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();

      try {
         return threadContextClassLoader.loadClass(desc.getName());
      } catch (ClassNotFoundException cnfe) {
      }

      return super.resolveClass(desc);
   }
}
