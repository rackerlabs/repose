package com.rackspace.papi.components.identity.parsers;

import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.wrappers.CredentialsWrapper;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;

public class JsonAuthBodyParser {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(JsonAuthBodyParser.class);
   private final JacksonJaxbTransform transform;
   
   public JsonAuthBodyParser(JacksonJaxbTransform transform) {
      this.transform = transform;
   }
   
   public CredentialsWrapper parse(InputStream stream) {
      try {
         return parse(new String(RawInputStreamReader.instance().readFully(stream)));
      } catch(IOException ex) {
         LOG.warn("Error reading JSON stream", ex);
      }
      
      return null;
   }
   
   public CredentialsWrapper parse(String body) {
      for (Class<? extends CredentialsWrapper> wrapper: CredentialsWrapper.wrappers) {
         CredentialsWrapper candidate = transform.deserialize(body, wrapper);
         if (candidate != null) {
            return candidate;
         }
      }
      
      return null;
   }
}
