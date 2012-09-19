package com.rackspace.papi.httpx.processor;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.httpx.processor.cdata.UnknownContentStreamProcessor;
import com.rackspace.papi.httpx.processor.common.InputStreamProcessor;
import com.rackspace.papi.httpx.processor.json.JsonxStreamProcessor;
import com.rackspace.papi.httpx.processor.util.BodyContentMediaType;
import org.codehaus.jackson.JsonFactory;

import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.IOException;
import java.io.InputStream;

public class TranslationResponsePreProcessor {

   private static final SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
   private final MutableHttpServletResponse response;
   private final boolean jsonPreprocessing;

   public TranslationResponsePreProcessor(MutableHttpServletResponse response, boolean jsonPreprocessing) {
      this.response = response;
      this.jsonPreprocessing = jsonPreprocessing;
   }

   public InputStream getBodyStream() throws IOException {
      final String contentType = response.getContentType();
      final InputStream result;

      switch (BodyContentMediaType.getMediaType(contentType)) {
         case XML:
            result = response.getInputStream();
            break;
         case JSON:
            if (jsonPreprocessing) {
                result = getJsonProcessor().process(response.getInputStream());
            } else {
                result = response.getInputStream();
            }
            break;
         default:
            result = getUnknownContentProcessor().process(response.getInputStream());
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
