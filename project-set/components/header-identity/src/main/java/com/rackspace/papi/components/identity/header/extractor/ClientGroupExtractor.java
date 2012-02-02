package com.rackspace.papi.components.identity.header.extractor;

import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import javax.servlet.http.HttpServletRequest;

public class ClientGroupExtractor {
   public static final String DEST_GROUP = "IP_Standard";
   private final HttpServletRequest request;
   private final HeaderIdentityConfig config;

   public ClientGroupExtractor(HttpServletRequest request, HeaderIdentityConfig config) {
      this.request = request;
      this.config = config;
   }

   public  String determineIpGroup(String address) {
      return DEST_GROUP;
   }
}
