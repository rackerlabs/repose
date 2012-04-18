package org.openrepose.rackspace.auth_2_0.identity.parsers.json;

import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps.CredentialMap;
import org.openrepose.rackspace.auth_2_0.identity.parsers.AuthContentParser;
import org.slf4j.Logger;

import java.io.InputStream;

public class JsonMapAuthContentParser implements AuthContentParser {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(JsonMapAuthContentParser.class);
   private final JacksonJaxbTransform transform;

   public JsonMapAuthContentParser(JacksonJaxbTransform transform) {
      this.transform = transform;
   }

   @Override
   public AuthCredentials parse(InputStream stream) {
      CredentialMap credentials = transform.deserialize(stream, CredentialMap.class);

      // TODO
//      return credentials != null? credentials.getCredentials(): null;
      return null;
   }

   @Override
   public AuthCredentials parse(String content) {
      CredentialMap credentials = transform.deserialize(content, CredentialMap.class);

//      return credentials != null? credentials.getCredentials(): null;
      return null;
   }
}
