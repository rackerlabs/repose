package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.components.translation.preprocessor.InputStreamProcessor;
import com.rackspace.papi.components.translation.preprocessor.cdata.UnknownContentStreamProcessor;
import com.rackspace.papi.components.translation.preprocessor.json.JsonxStreamProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.codehaus.jackson.JsonFactory;

public class TranslationRequestPreProcessor {

   private static final SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
   private final MutableHttpServletRequest request;

   public TranslationRequestPreProcessor(MutableHttpServletRequest request) {
      this.request = request;
   }

   private static enum MediaType {
      
      XML("(?i)[^/]+/(?:[^;]+\\.)*" + MediaType.XML_VALUE + "(?:\\.[^;]+)*(?:$|;.*)"),   // matches strings like application/xml or application/vendor.xml.somestring;param=1
      JSON("(?i)[^/]+/(?:[^;]+\\.)*" + MediaType.JSON_VALUE + "(?:\\.[^;]+)*(?:$|;.*)"), // matches strings like application/json or application/vendor.json.somestring;param=1
      UNKNOWN(".*");
      
      private static final String JSON_VALUE = "json";
      private static final String XML_VALUE = "xml";
      private final Pattern pattern;

      MediaType(String regex) {
         pattern = Pattern.compile(regex);
      }

      Pattern getPattern() {
         return pattern;
      }

      public static MediaType getMediaType(String contentType) {
         for (MediaType mediaType : values()) {
            if (mediaType.getPattern().matcher(contentType).matches()) {
               return mediaType;
            }
         }

         return UNKNOWN;
      }
   }

   protected InputStream getHeaderStream() throws IOException {
      // TODO: plug in HTTPx here!!!!
      return null;
   }

   protected InputStream getBodyStream() throws IOException {
      final String contentType = request.getContentType();
      final InputStream result;

      switch (MediaType.getMediaType(contentType)) {
         case XML:
            result = request.getInputStream();
            break;
         case JSON:
            result = getJsonProcessor().process(request.getInputStream());
            break;
         default:
            result = getUnknownContentProcessor().process(request.getInputStream());
      }

      return result;
   }

   public InputStreamProcessor getJsonProcessor() {
      return new JsonxStreamProcessor(new JsonFactory(), handlerFactory);
   }
   
   public InputStreamProcessor getUnknownContentProcessor() {
      return new UnknownContentStreamProcessor();
   }
   
    // TODO: Need to rework this now that the config changed
//   public InputStream getSourceStream(HttpElement httpElement) throws IOException {
//
////      switch (httpElement) {
////         case ENVELOPE:
////            return getHeaderStream();
////         case BODY:
////            return getBodyStream();
////         case ALL:
////            return InputStreamMerger.merge(
////                    InputStreamMerger.wrap(REQUEST_PREFIX),
////                    getHeaderStream(),
////                    getBodyStream(),
////                    InputStreamMerger.wrap(REQUEST_SUFFIX));
////      }
//
//      return null;
//   }
}
