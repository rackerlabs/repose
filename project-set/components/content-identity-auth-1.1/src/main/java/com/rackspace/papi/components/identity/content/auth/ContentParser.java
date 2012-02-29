package com.rackspace.papi.components.identity.content.auth;

import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import com.rackspace.papi.components.identity.parsers.json.JsonMapAuthContentParser;
import com.rackspace.papi.components.identity.parsers.xml.XmlAuthContentParser;
import com.rackspacecloud.docs.auth.api.v1.Credentials;

import javax.xml.bind.JAXBElement;
import java.io.InputStream;

public class ContentParser {

   private final JacksonJaxbTransform jsonTranformer;
   private Transform<InputStream, JAXBElement<Credentials>> xmlTransformer;

   public ContentParser(JacksonJaxbTransform jsonTranformer, Transform<InputStream, JAXBElement<Credentials>> xmlTransformer) {
      this.jsonTranformer = jsonTranformer;
      this.xmlTransformer = xmlTransformer;
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
      return new XmlAuthContentParser(xmlTransformer);
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
