package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.header.QualityFactorUtility;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.FileReader;
import com.rackspace.papi.commons.util.io.FileReaderImpl;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;

import com.rackspace.papi.service.config.ConfigurationService;

import com.rackspace.papi.service.rms.config.Message;
import com.rackspace.papi.service.rms.config.OverwriteType;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import com.rackspace.papi.service.rms.config.StatusCodeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseMessageServiceImpl implements ResponseMessageService {

   private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageServiceImpl.class);
   private static final Pattern URI_PATTERN = Pattern.compile(":\\/\\/");
   private final UpdateListener<ResponseMessagingConfiguration> updateMessageConfig;
   private final List<StatusCodeMatcher> statusCodeMatcherList;
   private final Map<String, Pattern> statusCodeRegexes;
   private final Map<String, HttpLogFormatter> formatTemplates;
   private final KeyedStackLock configurationLock;
   private final Object read, update;

   public ResponseMessageServiceImpl(ConfigurationService configurationService) {
      statusCodeMatcherList = new LinkedList<StatusCodeMatcher>();
      statusCodeRegexes = new HashMap<String, Pattern>();
      formatTemplates = new HashMap<String, HttpLogFormatter>();

      configurationLock = new KeyedStackLock();
      read = new Object();
      update = new Object();

      // Modern java programming. No one ever said it was pretty.
      updateMessageConfig = new UpdateListener<ResponseMessagingConfiguration>() {

         @Override
         public void configurationUpdated(ResponseMessagingConfiguration configurationObject) {
            configurationLock.lock(update);

            try {
               statusCodeMatcherList.clear();
               statusCodeMatcherList.addAll(configurationObject.getStatusCode());

               formatTemplates.clear();

               for (StatusCodeMatcher code : statusCodeMatcherList) {
                  statusCodeRegexes.put(code.getId(), Pattern.compile(code.getCodeRegex()));
               }
            } finally {
               configurationLock.unlock(update);
            }
         }
      };

      configurationService.subscribeTo("response-messaging.cfg.xml", updateMessageConfig, ResponseMessagingConfiguration.class);
   }

   @Override
   public void destroy() {
      // Nothing that a good de-referencing can't clean up.
   }

   @Override
   public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
      handle("", request, response);
   }

   @Override
   public void handle(String message, HttpServletRequest request, HttpServletResponse response) throws IOException {
      // In the case where we pass/route the request, there is a chance that
      // the repsonse will be committed by and underlying service, outside of papi
      if (response.isCommitted()) {
         return;
      }

      final StatusCodeMatcher matchedCode = getMatchingStatusCode(String.valueOf(response.getStatus()));

      if (matchedCode != null) {
         final List<MediaType> mediaRanges = new MediaRangeParser(request.getHeaders(CommonHttpHeader.ACCEPT.toString())).parse();
         final MediaType preferedMediaRange = QualityFactorUtility.choosePreferredHeaderValue(mediaRanges);

         final Message statusCodeMessage = getMatchingStatusCodeMessage(matchedCode, preferedMediaRange);

         if (statusCodeMessage != null) {

            final HttpLogFormatter formatter = getFormatter(matchedCode, statusCodeMessage);

            if (formatter != null) {
               final String formattedOutput = formatter.format(message, request, response).trim();

               final boolean emptyBody = !responseHasBody(response);

               if (!(StringUtilities.nullSafeEqualsIgnoreCase(matchedCode.getOverwrite().value(), OverwriteType.IF_EMPTY.value()) &&
                   !emptyBody)) {
                  // overwrite body
                  response.resetBuffer();
                  response.setContentLength(formattedOutput.length());
                  response.setHeader(CommonHttpHeader.CONTENT_TYPE.toString(), preferedMediaRange.getMimeType().toString());

                  // TODO:Enhancement - Update formatter logic for streaming
                  // TODO:Enhancement - Update getBytes(...) to use requested content encoding
                  response.getOutputStream().write(formattedOutput.getBytes());
               }
            } else{
               LOG.info("No formatter found for message code.  Skipping Response Message Service formatting for status code regex " + matchedCode.getCodeRegex()); 
            }
         }
      }
   }

   private boolean responseHasBody(HttpServletResponse response) {
      boolean hasBody = false;

      MutableHttpServletResponse mutableResponse = (MutableHttpServletResponse) response;

      InputStream body = mutableResponse.getBufferedOutputAsInputStream();

      try {
         if (body != null && body.available() > 0) {
            hasBody = true;
         }
      } catch (IOException e) {
         hasBody = false;
      }

      return hasBody;
   }

   private StatusCodeMatcher getMatchingStatusCode(String statusCode) {
      StatusCodeMatcher matchedCode = null;

      configurationLock.lock(read);

      try {
         for (StatusCodeMatcher code : statusCodeMatcherList) {
            if (statusCodeRegexes.get(code.getId()).matcher(statusCode).matches()) {
               matchedCode = code;
               break;
            }
         }
      } finally {
         configurationLock.unlock(read);
      }

      return matchedCode;
   }

   private HttpLogFormatter getFormatter(StatusCodeMatcher code, Message message) {
      HttpLogFormatter formatter = null;
      final String messageKey = code.getId() + message.getMediaType();

      configurationLock.lock(update);

      try {
         formatter = formatTemplates.get(messageKey);

         if (formatter == null) {
            final String href = message.getHref();
            final String stringTemplate = !StringUtilities.isBlank(href) ? readHref(href, code) : message.getValue();

            formatter = new HttpLogFormatter(stringTemplate);
            formatTemplates.put(messageKey, formatter);
         }
      } finally {
         configurationLock.unlock(update);
      }

      return formatter;
   }

   //TODO:Enhancement Update the service to use a uri resolver
   private String readHref(String href, StatusCodeMatcher code) {
      String stringMessage = "";

      final File f = getFileFromHref(href, code);

      if (f != null) {
         final FileReader fin = new FileReaderImpl(f);

         try {
            stringMessage = fin.read();
         } catch (IOException ioe) {
            LOG.error(StringUtilities.join("Failed to read file: ", f.getAbsolutePath(), " - Reason: ", ioe.getMessage()), ioe);
         }
      }

      return stringMessage;
   }

   private File getFileFromHref(String messageHref, StatusCodeMatcher code) {
      File f = null;

      final Matcher m = URI_PATTERN.matcher(messageHref);

      if (m.find() && messageHref.startsWith("file://")) {
         try {
            f = new File(new URI(messageHref));
         } catch (URISyntaxException urise) {
            LOG.error("Bad URI syntax in message href for status code: " + code.getId(), urise);
         }
      }

      return f;
   }

   private Message getMatchingStatusCodeMessage(StatusCodeMatcher code, MediaType requestedMediaType) {
      Message wildcard = null;
      for (Message message : code.getMessage()) {
         MediaType mediaType = new MediaType(requestedMediaType.getValue(), MimeType.getMatchingMimeType(message.getMediaType()), requestedMediaType.getParameters());
         if (mediaType.equals(requestedMediaType)) {
            return message;
         }
         // A configured wildcard (*/*) will be returned if an exact match is not found
         if (mediaType.getMimeType().equals(MimeType.WILDCARD)) {
            wildcard = message;
         }
      }

      return wildcard;
   }
}
