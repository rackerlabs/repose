package com.rackspace.papi.components.clientauth.common;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public class UriMatcher {

   private final List<Pattern> whiteListRegexPatterns;

   public UriMatcher(List<Pattern> whiteListRegexPatterns) {
      this.whiteListRegexPatterns = whiteListRegexPatterns;
   }

   public boolean isUriOnWhiteList(String requestUri) {
      boolean matches = false;

      for (Pattern pattern : whiteListRegexPatterns) {
         if (pattern.matcher(requestUri).matches()) {
            matches = true;
            break;
         }
      }

      return matches;
   }

}
