package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.service.rms.config.Message;
import com.rackspace.papi.service.rms.config.StatusCodeMatcher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public final class ImmutableRMSConfiguration {
   private final List<StatusCodeMatcher> statusCodeMatcherList = new LinkedList<StatusCodeMatcher>();
   private final Map<String, Pattern> statusCodeRegexes = new HashMap<String, Pattern>();
   private final Map<String, HttpLogFormatter> formatTemplates = new HashMap<String, HttpLogFormatter>();

   private ImmutableRMSConfiguration(List<StatusCodeMatcher> statusCodes) {
      updateStatusCodes(statusCodes);
      buildCompiledRegexPatterns();
      buildFormatTemplates(statusCodes);
   }

   private void updateStatusCodes(List<StatusCodeMatcher> statusCodes) {
      statusCodeMatcherList.clear();
      statusCodeMatcherList.addAll(statusCodes);
   }

   private void buildCompiledRegexPatterns() {
      statusCodeRegexes.clear();
      for (StatusCodeMatcher code : statusCodeMatcherList) {
         statusCodeRegexes.put(code.getId(), Pattern.compile(code.getCodeRegex()));
      }
   }

   private void buildFormatTemplates(List<StatusCodeMatcher> statusCodes) {
      formatTemplates.clear();
      for (StatusCodeMatcher statusCode : statusCodes) {

         for (Message message : statusCode.getMessage()) {
            final String statusCodeId = statusCode.getId();
            final String href = message.getHref();
            final String stringTemplate = !StringUtilities.isBlank(href) ? new HrefFileReader().read(href, statusCodeId) : message.getValue();

            formatTemplates.put(statusCodeId + message.getMediaType(), new HttpLogFormatter(stringTemplate));
         }
      }
   }

   public HttpLogFormatter getMatchingLogFormatter(String statusCodeId, String mediaType) {
      return formatTemplates.get(statusCodeId + mediaType);
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

   public static ImmutableRMSConfiguration build(List<StatusCodeMatcher> statusCodeMatchers){
      return new ImmutableRMSConfiguration(statusCodeMatchers);
   }
}
