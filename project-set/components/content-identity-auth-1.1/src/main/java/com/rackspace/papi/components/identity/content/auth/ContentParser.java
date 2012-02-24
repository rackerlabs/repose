package com.rackspace.papi.components.identity.content.auth;

import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import com.rackspace.papi.components.identity.parsers.json.JsonMapAuthContentParser;
import java.io.InputStream;

public class ContentParser {

   private final JacksonJaxbTransform jsonTranformer;

   public ContentParser(JacksonJaxbTransform jsonTranformer) {
      this.jsonTranformer = jsonTranformer;
   }

   private boolean isJson(MimeType mimeType) {
      return mimeType != null && MimeType.APPLICATION_JSON.getSubType().equals(mimeType.getSubType());
   }

   private AuthContentParser getJsonContentParser() {
      return new JsonMapAuthContentParser(jsonTranformer);
   }

   public AuthCredentials parse(MimeType mimeType, InputStream content) {
      AuthCredentials credentials = null;

      if (isJson(mimeType)) {
         credentials = getJsonContentParser().parse(content);
      }
      // TODO extract XML

      return credentials;

   }
}
