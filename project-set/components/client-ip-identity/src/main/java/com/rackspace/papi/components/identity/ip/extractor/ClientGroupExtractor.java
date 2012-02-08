package com.rackspace.papi.components.identity.ip.extractor;

import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import javax.servlet.http.HttpServletRequest;

public class ClientGroupExtractor {
   public static final String DEST_GROUP = "IP_Standard";
   private final HttpServletRequest request;
   private final IpIdentityConfig config;

   public ClientGroupExtractor(HttpServletRequest request, IpIdentityConfig config) {
      this.request = request;
      this.config = config;
   }

   public  String determineIpGroup(String address) {
      return DEST_GROUP;
   }
}
