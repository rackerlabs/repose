package com.rackspace.papi.components.clientip.extractor;

import com.rackspace.papi.components.clientip.config.ClientIpIdentityConfig;
import javax.servlet.http.HttpServletRequest;

public class ClientGroupExtractor {
   public static final String DEST_GROUP = "IP_Standard";
   private final HttpServletRequest request;
   private final ClientIpIdentityConfig config;

   public ClientGroupExtractor(HttpServletRequest request, ClientIpIdentityConfig config) {
      this.request = request;
      this.config = config;
   }

   public  String determineIpGroup(String address) {
      return DEST_GROUP;
   }
}
