package org.openrepose.rackspace.auth_2_0.identity.content.auth;

import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.parsers.AuthContentParser;
import org.openrepose.rackspace.auth_2_0.identity.parsers.json.JsonMapAuthContentParser;
import org.openrepose.rackspace.auth_2_0.identity.parsers.xml.AuthenticationRequestParser;

import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

public class ContentParser {

   private final JacksonJaxbTransform jsonTranformer;
   private Unmarshaller unmarshaller;

   public ContentParser(JacksonJaxbTransform jsonTranformer, Unmarshaller unmarshaller) {
      this.jsonTranformer = jsonTranformer;
      this.unmarshaller = unmarshaller;
   }

   private boolean isJson(MimeType mimeType) {
      return mimeType != null && MimeType.APPLICATION_JSON.getSubType().equals(mimeType.getSubType());
   }

   private boolean isXml(MimeType mimeType) {
      return mimeType != null && MimeType.APPLICATION_XML.getSubType().equals(mimeType.getSubType());
   }

   private AuthContentParser getJsonContentParser() {
      return new JsonMapAuthContentParser(jsonTranformer);
   }

   private AuthContentParser getXmlContentParser() {
      return new AuthenticationRequestParser(unmarshaller);
   }

   public AuthCredentials parse(MimeType mimeType, InputStream content) {
      AuthCredentials credentials = null;

      if (isJson(mimeType)) {
         credentials = getJsonContentParser().parse(content);
      } else if (isXml(mimeType)) {
         credentials = getXmlContentParser().parse(content);
      }

      return credentials;

   }
}
