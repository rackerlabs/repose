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

import com.rackspace.papi.service.rms.config.OverwriteType;
import com.rackspace.papi.service.rms.config.StatusCodeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ResponseMessageServiceImpl implements ResponseMessageService {

   private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageServiceImpl.class);
   private static final HeaderChooser<MediaType> ACCEPT_TYPE_CHOOSER = new QualityFactorHeaderChooser<MediaType>(new MediaType(MimeType.WILDCARD, -1));

   private List<StatusCodeMatcher> statusCodeMatcherList = new LinkedList<StatusCodeMatcher>();
   private Map<String, Pattern> statusCodeRegexes = new HashMap<String, Pattern>();
   private Map<String, HttpLogFormatter> formatTemplates = new HashMap<String, HttpLogFormatter>();

   private final KeyedStackLock configurationLock = new KeyedStackLock();
   private final Object readKey = new Object();
   private final Object updateKey = new Object();

   @Override
   public void destroy() {
      // Nothing that a good de-referencing can't clean up.
   }

   @Override
   public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
      
      final StatusCodeMatcher matchedCode = getMatchingStatusCode(String.valueOf(response.getStatus()));

      if (matchedCode != null) {
         final HttpLogFormatGenerator formatGenerator = new HttpLogFormatGenerator(configurationLock, updateKey, formatTemplates);
         final MediaType preferredMediaType = ACCEPT_TYPE_CHOOSER.choosePreferredHeaderValue(new MediaRangeParser(request.getHeaders("Accept")).parse());
         final HttpLogFormatter formatter = formatGenerator.generate(matchedCode, preferredMediaType);

         if (formatter != null) {

            if (!(configSetToIfEmpty(matchedCode) && hasBody(response))) {

               final String formattedOutput = formatter.format("", request, response).trim();

               // overwrite body
               response.resetBuffer();
               response.setContentLength(formattedOutput.length());
               response.setHeader(CommonHttpHeader.CONTENT_TYPE.toString(), preferredMediaType.getMimeType().toString());

               // TODO:Enhancement - Update formatter logic for streaming
               // TODO:Enhancement - Update getBytes(...) to use requested content encoding
               response.getOutputStream().write(formattedOutput.getBytes());
            }
         } else{
            LOG.info("No formatter found for message code.  Skipping Response Message Service formatting for status code regex " + matchedCode.getCodeRegex());
         }
      }
   }

   public boolean configSetToIfEmpty(StatusCodeMatcher matchedCode) {
      return StringUtilities.nullSafeEqualsIgnoreCase(matchedCode.getOverwrite().value(), OverwriteType.IF_EMPTY.value());
   }

   public boolean hasBody(HttpServletResponse response) {
      return ((MutableHttpServletResponse)response).hasBody();
   }

   public void updateConfiguration(List<StatusCodeMatcher> statusCodeMatchers) {
      configurationLock.lock(updateKey);

      try {
         formatTemplates.clear();

         statusCodeMatcherList.clear();
         statusCodeMatcherList.addAll(statusCodeMatchers);

         statusCodeRegexes.clear();
         for (StatusCodeMatcher code : statusCodeMatcherList) {
            statusCodeRegexes.put(code.getId(), Pattern.compile(code.getCodeRegex()));
         }
      } finally {
         configurationLock.unlock(updateKey);
      }
   }

   public StatusCodeMatcher getMatchingStatusCode(String statusCode) {
      StatusCodeMatcher matchedCode = null;

      configurationLock.lock(readKey);

      try {
         for (StatusCodeMatcher code : statusCodeMatcherList) {
            if (statusCodeRegexes.get(code.getId()).matcher(statusCode).matches()) {
               matchedCode = code;
               break;
            }
         }
      } finally {
         configurationLock.unlock(readKey);
      }

      return matchedCode;
   }
}
