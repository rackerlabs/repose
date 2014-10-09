package org.openrepose.commons.utils.digest.impl;

import org.openrepose.commons.utils.digest.AbstractMessageDigester;

public class MD5MessageDigester extends AbstractMessageDigester {

   @Override
   protected String digestSpecName() {
      return "MD5";
   }
}
