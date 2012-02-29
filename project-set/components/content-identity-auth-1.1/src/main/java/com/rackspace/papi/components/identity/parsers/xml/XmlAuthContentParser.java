package com.rackspace.papi.components.identity.parsers.xml;

import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.content.credentials.maps.CredentialMap;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import org.slf4j.Logger;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fran
 */
public class XmlAuthContentParser implements AuthContentParser {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlAuthContentParser.class);
   private final Transform<InputStream, JAXBElement<Credentials>> xmlTransformer;

   public XmlAuthContentParser(Transform<InputStream, JAXBElement<Credentials>> xmlTransformer) {
      this.xmlTransformer = xmlTransformer;
   }

   @Override
   public AuthCredentials parse(InputStream stream) {
      final CredentialMap credentialMap = new CredentialMap();
      final Map attributesMap = new HashMap();

      JAXBElement<Credentials> jaxbCredentials = xmlTransformer.transform(stream);

      Credentials credentials = jaxbCredentials.getValue();

      System.out.println(credentials.getClass().getName());

      return credentialMap.getCredentials();
   }

   @Override
   public AuthCredentials parse(String content) {
      return this.parse(new ByteArrayInputStream(content.getBytes()));
   }
}
