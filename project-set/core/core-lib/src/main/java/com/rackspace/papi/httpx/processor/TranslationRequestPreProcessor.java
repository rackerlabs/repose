package com.rackspace.papi.httpx.processor;

import com.rackspace.papi.httpx.processor.cdata.UnknownContentStreamProcessor;
import com.rackspace.papi.httpx.processor.common.InputStreamProcessor;
import com.rackspace.papi.httpx.processor.json.JsonxStreamProcessor;
import com.rackspace.papi.httpx.processor.util.BodyContentMediaType;
import org.codehaus.jackson.JsonFactory;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.IOException;
import java.io.InputStream;

public class TranslationRequestPreProcessor {

   private static final SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
   private final HttpServletRequest request;
   private final boolean jsonPreprocessing;

   public TranslationRequestPreProcessor(HttpServletRequest request, boolean jsonPreprocessing) {
      this.request = request;
      this.jsonPreprocessing = jsonPreprocessing;
   }

   public InputStream getBodyStream() throws IOException {
      final String contentType = request.getContentType();
      final InputStream result;

      switch (BodyContentMediaType.getMediaType(contentType)) {
         case XML:
            result = request.getInputStream();
            break;
         case JSON:
            if (jsonPreprocessing) {
                result = getJsonProcessor().process(request.getInputStream());
            } else {
                result = request.getInputStream();
            }
            break;
         default:
            result = getUnknownContentProcessor().process(request.getInputStream());
      }

      return result;
   }

   protected InputStreamProcessor getJsonProcessor() {
      return new JsonxStreamProcessor(new JsonFactory(), handlerFactory);
   }
   
   protected InputStreamProcessor getUnknownContentProcessor() {
      return new UnknownContentStreamProcessor();
   }
   
}
