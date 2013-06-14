package com.rackspace.papi.components.identity.header.extractor;

import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.components.identity.header.config.HttpHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class HeaderValueExtractor {

   private HttpServletRequest request;

   public HeaderValueExtractor(HttpServletRequest request) {
      this.request = request;
   }

   protected String extractHeader(String name) {
      // Header value may be a comma separated list of values.
      // The left most value in the list will be extracted.
      String header = request.getHeader(name);
      return header != null ? header.split(",")[0].trim() : "";
   }

   public List<ExtractorResult<String>> extractUserGroup(List<HttpHeader> headerNames) {
      List<ExtractorResult<String>> results = new ArrayList<ExtractorResult<String>>();
      String user = "";
      String group = "";

      for (HttpHeader header : headerNames) {
         user = extractHeader(header.getId());
         if (!user.isEmpty()) {
            String quality = determineQuality(header);
            user += quality;
            group = header.getId() + quality;
            results.add(new ExtractorResult<String>(user, group));
         }
      }

      return results;
   }

   private String determineQuality(HttpHeader header) {
      return ";q=" + header.getQuality();
   }
}
