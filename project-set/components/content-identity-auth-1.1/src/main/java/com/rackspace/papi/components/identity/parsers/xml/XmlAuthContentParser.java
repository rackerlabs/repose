package com.rackspace.papi.components.identity.parsers.xml;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.content.credentials.maps.CredentialMap;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import org.slf4j.Logger;

import javax.xml.stream.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fran
 */
public class XmlAuthContentParser implements AuthContentParser {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlAuthContentParser.class);
   private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

   @Override
   public AuthCredentials parse(InputStream stream) {
      final CredentialMap credentialMap = new CredentialMap();
      final Map attributesMap = new HashMap();

      try {
         XMLStreamReader xmlStreamReader = inputFactory.createXMLStreamReader(stream);

         if (xmlStreamReader.hasNext()) {
            if (xmlStreamReader.next() == XMLStreamConstants.START_ELEMENT) {

               attributesMap.put(xmlStreamReader.getAttributeLocalName(0), xmlStreamReader.getAttributeValue(0));
               attributesMap.put(xmlStreamReader.getAttributeLocalName(1), xmlStreamReader.getAttributeValue(1));
               credentialMap.put(xmlStreamReader.getLocalName(), attributesMap);
            }
         }
      } catch (XMLStreamException e) {
         LOG.error("XMLStreamException when parsing auth credentials: " + e.getMessage());
      }

      return credentialMap.getCredentials();
   }

   @Override
   public AuthCredentials parse(String content) {
      return this.parse(new ByteArrayInputStream(content.getBytes()));
   }
}
