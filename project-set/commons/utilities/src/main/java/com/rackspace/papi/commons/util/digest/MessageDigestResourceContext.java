package com.rackspace.papi.commons.util.digest;

import com.rackspace.papi.commons.util.io.MessageDigesterOutputStream;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class MessageDigestResourceContext implements ResourceContext<MessageDigest, byte[]> {

   private final InputStream inputStream;
   private static final int BYTE_BUFFER_SIZE = 1024;

   public MessageDigestResourceContext(InputStream inputStream) {
      this.inputStream = inputStream;
   }

   @Override
   public byte[] perform(MessageDigest resource) throws ResourceContextException {
      final MessageDigesterOutputStream output = new MessageDigesterOutputStream(resource);
      final byte[] buffer = new byte[BYTE_BUFFER_SIZE];

      int read;

      try {
         while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
         }

         inputStream.close();
         output.close();
      } catch (IOException ioe) {
         throw new ResourceContextException("I/O Exception caught during input stream message digesting. Reason: " + ioe.getMessage(), ioe);
      }

      return output.getDigest();
   }
}
