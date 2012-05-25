package com.rackspace.papi.commons.util.digest;

import com.rackspace.papi.commons.util.ArrayUtilities;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;

import java.security.MessageDigest;

public class ByteArrayResourceContext implements ResourceContext<MessageDigest, byte[]> {

   private final byte[] byteArray;

   public ByteArrayResourceContext(byte[] byteArray) {
      this.byteArray = ArrayUtilities.nullSafeCopy(byteArray);
   }

   @Override
   public byte[] perform(MessageDigest resource) throws ResourceContextException {
      return resource.digest(byteArray);
   }
}
