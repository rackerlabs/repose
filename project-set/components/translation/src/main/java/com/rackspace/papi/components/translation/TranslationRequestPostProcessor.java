package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.components.translation.postprocessor.InputStreamPostProcessor;
import com.rackspace.papi.components.translation.postprocessor.RequestStreamPostProcessor;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.Source;

public class TranslationRequestPostProcessor {

   private final MutableHttpServletRequest request;

   public TranslationRequestPostProcessor(MutableHttpServletRequest request) {
      this.request = request;
   }

   public InputStream getBodyStream(Source node) throws IOException {
      return getPostProcessor().process(node);
   }
   
   protected InputStreamPostProcessor getPostProcessor() {
      return new RequestStreamPostProcessor();
   }
   
}
