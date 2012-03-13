package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.header.HeaderChooser;
import com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.MimeType;

import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;

import com.rackspace.papi.service.rms.config.Message;
import com.rackspace.papi.service.rms.config.OverwriteType;
import com.rackspace.papi.service.rms.config.StatusCodeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.List;

public class ResponseMessageServiceImpl implements ResponseMessageService {

   private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageServiceImpl.class);
   private static final HeaderChooser<MediaType> ACCEPT_TYPE_CHOOSER = new QualityFactorHeaderChooser<MediaType>(new MediaType(MimeType.WILDCARD, -1));

   private final KeyedStackLock configurationLock = new KeyedStackLock();
   private final Object updateKey = new Object();
   private final Object readKey = new Object();

   private ImmutableStatusCodes immutableStatusCodes;
   private ImmutableFormatTemplates immutableFormatTemplates;

   @Override
   public void destroy() {
      // Nothing that a good de-referencing can't clean up.
   }

   @Override
   public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
      
      final StatusCodeMatcher matchedCode = getMatchingStatusCode(String.valueOf(response.getStatus()));

      if (matchedCode != null) {
         final MediaType preferredMediaType = ACCEPT_TYPE_CHOOSER.choosePreferredHeaderValue(new MediaRangeParser(request.getHeaders("Accept")).parse());
         final HttpLogFormatter formatter = getHttpLogFormatter(matchedCode, preferredMediaType);

         if (formatter != null) {

            if (!(configSetToIfEmpty(matchedCode) && hasBody(response))) {

               final String formattedOutput = formatter.format("", request, response).trim();

               overwriteResponseBody(response, formattedOutput, preferredMediaType.getMimeType().toString());
            }
         } else{
            LOG.info("No formatter found for message code.  Skipping Response Message Service formatting for status code regex " + matchedCode.getCodeRegex());
         }
      }
   }

   public void updateConfiguration(List<StatusCodeMatcher> statusCodeMatchers) {
      configurationLock.lock(updateKey);

      try {
        immutableStatusCodes = ImmutableStatusCodes.build(statusCodeMatchers);
        immutableFormatTemplates = ImmutableFormatTemplates.build(statusCodeMatchers);
      } finally {
         configurationLock.unlock(updateKey);
      }
   }

   private HttpLogFormatter getHttpLogFormatter(StatusCodeMatcher matchedCode, MediaType preferredMediaType) {
      HttpLogFormatter httpLogFormatter = null;

      if (matchedCode != null && preferredMediaType != null) {
         final Message message = MessageFilter.filterByMediaType(matchedCode.getMessage(), preferredMediaType);

         configurationLock.lock(readKey);

         try {
            httpLogFormatter = immutableFormatTemplates.getMatchingLogFormatter(matchedCode.getId(), message.getMediaType());
         } finally {
            configurationLock.unlock(readKey);
         }
      }

      return httpLogFormatter;
   }

   private StatusCodeMatcher getMatchingStatusCode(String responseCode) {
      StatusCodeMatcher matchedCode = null;

      configurationLock.lock(readKey);

      try {
         if (immutableStatusCodes != null) {
            matchedCode = immutableStatusCodes.getMatchingStatusCode(responseCode);
         }
      } finally {
         configurationLock.unlock(readKey);
      }

      return matchedCode;
   }

   private void overwriteResponseBody(HttpServletResponse response, final String formattedOutput, String mediaType) throws IOException {
      response.resetBuffer();
      response.setContentLength(formattedOutput.length());
      response.setHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mediaType);

      // TODO:Enhancement - Update formatter logic for streaming
      // TODO:Enhancement - Update getBytes(...) to use requested content encoding
      response.getOutputStream().write(formattedOutput.getBytes());
   }

   private boolean configSetToIfEmpty(StatusCodeMatcher matchedCode) {
      return StringUtilities.nullSafeEqualsIgnoreCase(matchedCode.getOverwrite().value(), OverwriteType.IF_EMPTY.value());
   }

   private boolean hasBody(HttpServletResponse response) {
      return ((MutableHttpServletResponse)response).hasBody();
   }      
}
