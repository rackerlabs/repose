package com.rackspace.papi.components.identity.parsers.json;

import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.content.credentials.maps.CredentialMap;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import java.io.InputStream;
import org.slf4j.Logger;

public class JsonMapAuthContentParser implements AuthContentParser {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(JsonMapAuthContentParser.class);
   private final JacksonJaxbTransform transform;
   
   public JsonMapAuthContentParser(JacksonJaxbTransform transform) {
      this.transform = transform;
   }

   @Override
   public AuthCredentials parse(InputStream stream) {
      CredentialMap credentials = transform.deserialize(stream, CredentialMap.class);
      
      return credentials != null? credentials.getCredentials(): null;
   }
   
   @Override
   public AuthCredentials parse(String content) {
      CredentialMap credentials = transform.deserialize(content, CredentialMap.class);
      
      return credentials != null? credentials.getCredentials(): null;
   }
   
}
