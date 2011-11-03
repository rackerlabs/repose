package com.rackspace.papi.components.translation;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.RequestHeadDetail;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.components.translation.preprocessor.InputStreamProcessor;
import com.rackspace.papi.components.translation.preprocessor.cdata.UnknownContentStreamProcessor;
import com.rackspace.papi.components.translation.preprocessor.json.JsonxStreamProcessor;
import com.rackspace.papi.components.translation.util.BodyContentMediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.SAXTransformerFactory;

import com.rackspace.papi.httpx.parser.RequestParserFactory;
import org.codehaus.jackson.JsonFactory;

public class TranslationRequestPreProcessor {

   private static final SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
   private final MutableHttpServletRequest request;

   public TranslationRequestPreProcessor(MutableHttpServletRequest request) {
      this.request = request;
   }

   public InputStream getBodyStream() throws IOException {
      final String contentType = request.getContentType();
      final InputStream result;

      switch (BodyContentMediaType.getMediaType(contentType)) {
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

   protected InputStreamProcessor getJsonProcessor() {
      return new JsonxStreamProcessor(new JsonFactory(), handlerFactory);
   }
   
   protected InputStreamProcessor getUnknownContentProcessor() {
      return new UnknownContentStreamProcessor();
   }
   
}
