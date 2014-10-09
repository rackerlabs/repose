package org.openrepose.commons.utils.digest.impl;

import org.openrepose.commons.utils.digest.AbstractMessageDigester;

public class SHA1MessageDigester extends AbstractMessageDigester {

   @Override
   protected String digestSpecName() {
      return "SHA-1";
   }
}
