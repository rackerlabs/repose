package org.openrepose.rackspace.auth_2_0.identity.parsers.xml;

import com.rackspace.papi.commons.util.transform.Transform;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps.CredentialFactory;
import org.openrepose.rackspace.auth_2_0.identity.parsers.AuthContentParser;
import org.slf4j.Logger;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class XmlAuthContentParser implements AuthContentParser {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlAuthContentParser.class);

   private final Transform<InputStream, JAXBElement<?>> xmlTransformer;

   public XmlAuthContentParser(Transform<InputStream, JAXBElement<?>> xmlTransformer) {
      this.xmlTransformer = xmlTransformer;
   }

   @Override
   public AuthCredentials parse(InputStream stream) {

      JAXBElement<?> jaxbCredentials = xmlTransformer.transform(stream);

      return CredentialFactory.getCredentials(jaxbCredentials != null ? jaxbCredentials.getValue() : null);
   }

   @Override
   public AuthCredentials parse(String content) {
      return this.parse(new ByteArrayInputStream(content.getBytes()));
   }
   
}
