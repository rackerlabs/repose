/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.filters.headeridmapping.extractor;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.filters.headeridmapping.config.HttpHeader;

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
