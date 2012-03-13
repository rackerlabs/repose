package com.rackspace.papi.service.rms;

import com.rackspace.papi.service.rms.config.StatusCodeMatcher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public final class ImmutableStatusCodes {
   private final List<StatusCodeMatcher> statusCodeMatcherList = new LinkedList<StatusCodeMatcher>();
   private final Map<String, Pattern> statusCodeRegexes = new HashMap<String, Pattern>();

   private ImmutableStatusCodes(List<StatusCodeMatcher> statusCodes) {
      statusCodeMatcherList.clear();
      statusCodeMatcherList.addAll(statusCodes);
      
      statusCodeRegexes.clear();
      for (StatusCodeMatcher code : statusCodeMatcherList) {
         statusCodeRegexes.put(code.getId(), Pattern.compile(code.getCodeRegex()));
      }
   }

   public StatusCodeMatcher getMatchingStatusCode(String statusCode) {
      StatusCodeMatcher matchedCode = null;

      for (StatusCodeMatcher code : statusCodeMatcherList) {
         if (statusCodeRegexes.get(code.getId()).matcher(statusCode).matches()) {
            matchedCode = code;
            break;
         }
      }      

      return matchedCode;
   }

   public static ImmutableStatusCodes build(List<StatusCodeMatcher> statusCodeMatchers){
      return new ImmutableStatusCodes(statusCodeMatchers);
   }
}
