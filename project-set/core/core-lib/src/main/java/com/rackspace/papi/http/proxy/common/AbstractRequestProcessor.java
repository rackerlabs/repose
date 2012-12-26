package com.rackspace.papi.http.proxy.common;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class AbstractRequestProcessor {

   private static final String[] EXCLUDE_HEADERS = {"connection", "expect", "transfer-encoding", "content-length"};
   private static final Set<String> EXCLUDE_HEADERS_SET = new TreeSet<String>(Arrays.asList(EXCLUDE_HEADERS));
   
   protected boolean excludeHeader(String header) {
      return EXCLUDE_HEADERS_SET.contains(header.toLowerCase());
   }
}
