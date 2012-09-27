package com.rackspace.papi.commons.util.digest.impl;

import com.rackspace.papi.commons.util.digest.AbstractMessageDigester;

public class SHA1MessageDigester extends AbstractMessageDigester {

   @Override
   protected String digestSpecName() {
      return "SHA-1";
   }
}
