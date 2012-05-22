package com.rackspace.papi.commons.util.logging.apache;

import com.rackspace.papi.commons.util.logging.apache.constraint.StatusCodeConstraint;
import com.rackspace.papi.commons.util.logging.apache.format.FormatArgumentHandler;
import com.rackspace.papi.commons.util.logging.apache.format.LogArgumentFormatter;
import com.rackspace.papi.commons.util.logging.apache.format.stock.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rackspace.papi.commons.util.StringUtilities.isBlank;
import static com.rackspace.papi.commons.util.StringUtilities.isEmpty;

public class HttpLogFormatter {

   private static final Pattern TABS = Pattern.compile("\\\\t+");
   private static final Pattern NEWLINES = Pattern.compile("\\\\n+");
   private final String formatTemplate;
   private final List<FormatArgumentHandler> handlerList;

   public HttpLogFormatter(String formatTemplate) {
      this.formatTemplate = handleTabsAndNewlines(formatTemplate);
      handlerList = new LinkedList<FormatArgumentHandler>();

      build();
   }

   private String handleTabsAndNewlines(String formatTemplate) {
      Matcher tabsMatcher = TABS.matcher(formatTemplate);
      Matcher newlinesMatcher = NEWLINES.matcher(tabsMatcher.replaceAll("\t"));

      return newlinesMatcher.replaceAll("\n");
   }

   private void build() {
      final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(formatTemplate);

      int previousTokenEnd = 0;

      while (m.find()) {
         handleStringContent(previousTokenEnd, m.start(), handlerList);
         handlerList.add(handleArgument(new LogArgumentGroupExtractor(m)));
         previousTokenEnd = m.end();
      }

      handleStringContent(previousTokenEnd, formatTemplate.length(), handlerList);
   }

   private void handleStringContent(int previousTokenEnd, int currentTokenStart, List<FormatArgumentHandler> argHandlerList) {
      final String betweenElements = formatTemplate.substring(previousTokenEnd, currentTokenStart);

      if (!isEmpty(betweenElements)) {
         argHandlerList.add(handleArgument(LogArgumentGroupExtractor.stringEntity(betweenElements)));
      }
   }

   private LogArgumentFormatter handleArgument(LogArgumentGroupExtractor extractor) {
      final LogArgumentFormatter argFormatter = new LogArgumentFormatter();

      if (!isBlank(extractor.getStatusCodes())) {
         argFormatter.setStatusCodeConstraint(new StatusCodeConstraint(extractor.getStatusCodes()));
      }

      setLogic(extractor, argFormatter);
      
      return argFormatter;
   }

   public static void setLogic(final LogArgumentGroupExtractor extractor, final LogArgumentFormatter formatter) {
      switch (LogFormatArgument.fromString(extractor.getEntity())) {
         case REQUEST_HEADER:
            formatter.setLogic(new RequestHeaderHandler(extractor.getVariable(), extractor.getArguments()));
            break;
         case RESPONSE_HEADER:
            formatter.setLogic(new ResponseHeaderHandler(extractor.getVariable(), extractor.getArguments()));
            break;
         case CANONICAL_PORT:
            formatter.setLogic(new CanonicalPortHandler());
            break;
         case LOCAL_ADDRESS:
            formatter.setLogic(new LocalAddressHandler());
            break;
         case STATUS_CODE:
            formatter.setLogic(new StatusCodeHandler());
            break;
         case QUERY_STRING:
            formatter.setLogic(new QueryStringHandler());
            break;
         case REMOTE_ADDRESS:
            formatter.setLogic(new RemoteAddressHandler());
            break;
         case REMOTE_HOST:
            formatter.setLogic(new RemoteHostHandler());
            break;
         case REMOTE_USER:
            formatter.setLogic(new RemoteUserHandler());
            break;
         case REQUEST_METHOD:
            formatter.setLogic(new RequestMethodHandler());
            break;
         case RESPONSE_BYTES:
            formatter.setLogic(new ResponseBytesHandler());
            break;
         case TIME_RECIEVED:
            formatter.setLogic(new TimeReceivedHandler());
            break;
         case URL_REQUESTED:
            formatter.setLogic(new UrlRequestedHandler());
            break;
         case PERCENT:
            formatter.setLogic(new StringHandler(LogFormatArgument.PERCENT.toString()));
            break;
         case STRING:
            formatter.setLogic(new StringHandler(extractor.getVariable()));
            break;
      }
   }

   List<FormatArgumentHandler> getHandlerList() {
      return new LinkedList<FormatArgumentHandler>(handlerList);
   }

   public String format(HttpServletRequest request, HttpServletResponse response) {
      return format("", request, response);
   }

   public String format(String message, HttpServletRequest request, HttpServletResponse response) {
      final StringBuilder builder = new StringBuilder(message);

      for (FormatArgumentHandler formatter : handlerList) {
         builder.append(formatter.format(request, response));
      }

      return builder.toString();
   }
}
