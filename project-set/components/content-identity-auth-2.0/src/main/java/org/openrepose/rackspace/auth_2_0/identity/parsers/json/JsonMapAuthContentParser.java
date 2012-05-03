package org.openrepose.rackspace.auth_2_0.identity.parsers.json;

import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps.CredentialMap;
import org.openrepose.rackspace.auth_2_0.identity.parsers.AuthContentParser;

import java.io.InputStream;

public class JsonMapAuthContentParser implements AuthContentParser {
   private final JacksonJaxbTransform transform;

   public JsonMapAuthContentParser(JacksonJaxbTransform transform) {
      this.transform = transform;
   }

   @Override
   public AuthCredentials parse(InputStream stream) {
      //CredentialMap credentials = transform.deserialize(stream, CredentialMap.class);

      // TODO
//      return credentials != null? credentials.getCredentials(): null;
      return null;
   }

   @Override
   public AuthCredentials parse(String content) {
      //CredentialMap credentials = transform.deserialize(content, CredentialMap.class);

//      return credentials != null? credentials.getCredentials(): null;
      return null;
   }
}
