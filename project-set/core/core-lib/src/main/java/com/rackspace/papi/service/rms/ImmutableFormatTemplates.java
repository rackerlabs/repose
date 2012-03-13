package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.service.rms.config.Message;
import com.rackspace.papi.service.rms.config.StatusCodeMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fran
 */
public final class ImmutableFormatTemplates {

   private final Map<String, HttpLogFormatter> formatTemplates = new HashMap<String, HttpLogFormatter>();

   private ImmutableFormatTemplates(List<StatusCodeMatcher> statusCodes) {
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

   public static ImmutableFormatTemplates build(List<StatusCodeMatcher> statusCodes) {
      return new ImmutableFormatTemplates(statusCodes);      
   }
}
