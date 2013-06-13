package com.rackspace.papi.components.identity.header_mapping.extractor;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.components.identity.header_mapping.config.HttpHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class HeaderValueExtractor {

   private HttpServletRequest request;
   public static final String DEFAULT_QUALITY = "0.1";

   public HeaderValueExtractor(HttpServletRequest request) {
      this.request = request;
   }

   protected String extractHeader(String name) {
      // Header value may be a comma separated list of values.
      // The left most value in the list will be extracted.
      if (name == null) {
         return "";
      }

      String header = request.getHeader(name);
      return header != null ? header.split(",")[0].trim() : "";
   }

   public ExtractorResult<String> extractUserGroup(List<HttpHeader> headerNames) {
      String user = "";
      String group = "";

      for (HttpHeader header : headerNames) {
         String candidateUser = extractHeader(header.getUserHeader());
         String candidateGroup = extractHeader(header.getGroupHeader());

         if (!StringUtilities.isBlank(candidateUser)) {
            String quality = determineQuality(header);
            user = candidateUser + quality;
            
            if (!StringUtilities.isBlank(candidateGroup)) {
               group = candidateGroup + quality;
            }
            
            break;
         }
      }

      return new ExtractorResult<String>(user, group);
   }

   private String determineQuality(HttpHeader header) {
       return ";q=" + header.getQuality();
   }
}
