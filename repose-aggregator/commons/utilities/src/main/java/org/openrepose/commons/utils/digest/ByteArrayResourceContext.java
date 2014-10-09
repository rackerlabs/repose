package org.openrepose.commons.utils.digest;

import org.openrepose.commons.utils.ArrayUtilities;
import org.openrepose.commons.utils.pooling.ResourceContext;
import org.openrepose.commons.utils.pooling.ResourceContextException;

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
