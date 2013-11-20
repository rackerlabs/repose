package org.openrepose.rackspace.auth_2_0.identity.parsers.json;

import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.parsers.AuthContentParser;

import java.io.InputStream;

public class JsonMapAuthContentParser implements AuthContentParser {


   public JsonMapAuthContentParser(JacksonJaxbTransform transform) {

   }

   @Override
   public AuthCredentials parse(InputStream stream) {
      return null;
   }

   @Override
   public AuthCredentials parse(String content) {
      return null;
   }
}
