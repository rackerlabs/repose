package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.translation.preprocessor.InputStreamProcessor;
import com.rackspace.papi.components.translation.preprocessor.cdata.UnknownContentStreamProcessor;
import com.rackspace.papi.components.translation.preprocessor.json.JsonxStreamProcessor;
import com.rackspace.papi.components.translation.util.BodyContentMediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.codehaus.jackson.JsonFactory;

public class TranslationRequestPostProcessor {

   private static final SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
   private final MutableHttpServletResponse response;

   public TranslationRequestPostProcessor(MutableHttpServletResponse response) {
      this.response = response;
   }


   public InputStream getHeaderStream() throws IOException {
      return new ByteArrayInputStream(response.toXml().getBytes());
   }

   public InputStream getBodyStream() throws IOException {
      final String contentType = response.getContentType();
      final InputStream result;

      switch (BodyContentMediaType.getMediaType(contentType)) {
         case XML:
            result = response.getBufferedOutputAsInputStream();
            break;
         case JSON:
            result = getJsonProcessor().process(response.getBufferedOutputAsInputStream());
            break;
         default:
            result = getUnknownContentProcessor().process(response.getBufferedOutputAsInputStream());
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
