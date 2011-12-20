package com.rackspace.papi.components.clientip.extractor;

import com.rackspace.papi.components.clientip.config.HttpHeader;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ClientIpExtractor {
   private HttpServletRequest request;

   public ClientIpExtractor(HttpServletRequest request) {
      this.request = request;
   }
   
   protected String extractHeader(String name) {
      String header = request.getHeader(name);
      return header != null? header.trim(): "";
   }
   
   public String extractIpAddress(List<HttpHeader> headerNames) {
      String address = request.getRemoteAddr();

      for (HttpHeader header : headerNames) {
         String candidate = extractHeader(header.getId());

         if (candidate.length() > 0) {
            address = candidate;
            break;
         }
      }
      
      return address;
   }
}
