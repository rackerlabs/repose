package com.rackspace.papi.components.clientip.extractor;

import com.rackspace.papi.commons.iputil.IPAddressUtil;
import com.rackspace.papi.components.clientip.config.HttpHeader;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ClientIpExtractor {
   private HttpServletRequest request;

   public ClientIpExtractor(HttpServletRequest request) {
      this.request = request;
   }
   
   protected String extractHeader(String name) {
      // Header value may be a comma separated list of IP addresses.
      // The client IP should be the left most IP address in the list.
      String header = request.getHeader(name);
      return header != null? header.split(",")[0].trim(): "";
   }
   
   public String extractIpAddress(List<HttpHeader> headerNames) {
      String address = request.getRemoteAddr();

      for (HttpHeader header : headerNames) {
         String candidate = extractHeader(header.getId());

         if (IPAddressUtil.isIPv4LiteralAddress(candidate) || IPAddressUtil.isIPv6LiteralAddress(candidate)) {
            address = candidate;
            break;
         }
      }
      
      return address;
   }
}
