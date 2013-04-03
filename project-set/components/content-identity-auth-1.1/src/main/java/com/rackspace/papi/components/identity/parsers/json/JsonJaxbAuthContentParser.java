package com.rackspace.papi.components.identity.parsers.json;

import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.content.credentials.wrappers.CredentialsWrapper;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class JsonJaxbAuthContentParser implements AuthContentParser {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(JsonJaxbAuthContentParser.class);
   private final JacksonJaxbTransform transform;
   
   public JsonJaxbAuthContentParser(JacksonJaxbTransform transform) {
      this.transform = transform;
   }

   @Override
   public AuthCredentials parse(InputStream stream) {
      try {
         return parse(new String(RawInputStreamReader.instance().readFully(stream),CharacterSets.UTF_8));
      } catch(IOException ex) {
         LOG.warn("Error reading JSON stream. Reason: " + ex.getMessage(), ex);
      }
      
      return null;
   }
   
   @Override
   public AuthCredentials parse(String body) {
      for (Class<? extends CredentialsWrapper> wrapper: CredentialsWrapper.WRAPPERS) {
         CredentialsWrapper candidate = transform.deserialize(body, wrapper);
         if (candidate != null) {
            return candidate;
         }
      }
      
      return null;
   }
}
