package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.service.rms.config.Message;
import com.rackspace.papi.service.rms.config.StatusCodeMatcher;

import java.util.Map;

/**
 * @author fran
 */
public class HttpLogFormatGenerator {

   private final KeyedStackLock configurationLock;
   private final Object updateKey;
   private final Map<String, HttpLogFormatter> formatTemplates;

   public HttpLogFormatGenerator(KeyedStackLock configurationLock, Object updateKey, Map<String, HttpLogFormatter> formatTemplates) {
      this.configurationLock = configurationLock;
      this.updateKey = updateKey;
      this.formatTemplates = formatTemplates;
   }

   public HttpLogFormatter generate(StatusCodeMatcher matchedCode, MediaType preferredMediaType) {
      final Message message = getMatchingStatusCodeMessage(matchedCode, preferredMediaType);

      HttpLogFormatter formatter = null;

      if (matchedCode != null && message != null) {
         final String messageKey = matchedCode.getId() + message.getMediaType();

         configurationLock.lock(updateKey);

         try {
            formatter = formatTemplates.get(messageKey);

            if (formatter == null) {
               final String href = message.getHref();
               final String stringTemplate = !StringUtilities.isBlank(href) ? new HrefFileReader().read(href, matchedCode.getId()) : message.getValue();

               formatter = new HttpLogFormatter(stringTemplate);
               formatTemplates.put(messageKey, formatter);
            }
         } finally {
            configurationLock.unlock(updateKey);
         }
      }

      return formatter;
   }

   public Message getMatchingStatusCodeMessage(StatusCodeMatcher code, MediaType requestedMediaType) {
      Message wildcard = null;

      if (code != null && requestedMediaType != null) {
         for (Message message : code.getMessage()) {
            final String messageMediaType = message.getMediaType();
            if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, requestedMediaType.getValue())) {
               return message;
            }
            // A configured wildcard (*/*) will be returned if an exact match is not found
            if (StringUtilities.nullSafeEqualsIgnoreCase(messageMediaType, MimeType.WILDCARD.getMimeType())) {
               wildcard = message;
            }
         }
      }

      return wildcard;
   }
}
