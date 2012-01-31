package com.rackspace.papi.components.clientip.extractor;

import com.rackspace.papi.components.clientip.config.HttpHeader;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

// NOTE: This is a sun class we use for IP validation.  If a customer eventually wants to run Repose on a JVM
// that does not include the IPAddressUtil class, we can look in our git history and resurrect the copy we had
// included in our project or perhaps find another way at that time to validate IP address in a JVM independent
// fashion.
import sun.net.util.IPAddressUtil;

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
