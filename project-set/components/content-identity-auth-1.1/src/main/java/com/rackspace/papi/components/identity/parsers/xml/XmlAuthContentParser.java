package com.rackspace.papi.components.identity.parsers.xml;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.content.credentials.maps.CredentialFactory;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import com.rackspacecloud.docs.auth.api.v1.Credentials;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author fran
 */
public class XmlAuthContentParser implements AuthContentParser {
   private final Transform<InputStream, JAXBElement<Credentials>> xmlTransformer;

   public XmlAuthContentParser(Transform<InputStream, JAXBElement<Credentials>> xmlTransformer) {
      this.xmlTransformer = xmlTransformer;
   }

   @Override
   public AuthCredentials parse(InputStream stream) {

      JAXBElement<Credentials> jaxbCredentials = xmlTransformer.transform(stream);

      return CredentialFactory.getCredentials(jaxbCredentials != null ? jaxbCredentials.getValue() : null);
   }

   @Override
   public AuthCredentials parse(String content) {
      return this.parse(new ByteArrayInputStream(content.getBytes(CharacterSets.UTF_8)));
   }
}
