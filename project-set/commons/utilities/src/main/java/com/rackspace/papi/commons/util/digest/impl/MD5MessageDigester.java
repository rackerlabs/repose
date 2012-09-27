package com.rackspace.papi.commons.util.digest.impl;

import com.rackspace.papi.commons.util.digest.AbstractMessageDigester;

public class MD5MessageDigester extends AbstractMessageDigester {

   @Override
   protected String digestSpecName() {
      return "MD5";
   }
}
