package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import org.slf4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ReposeEntityResolver implements EntityResolver {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ReposeEntityResolver.class);
   private final EntityResolver parent;
   private final boolean allowEntities;

   ReposeEntityResolver(EntityResolver parent, boolean allowEntities) {
      this.parent = parent;
      this.allowEntities = allowEntities;
   }

   @Override
   public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
      LOG.warn((allowEntities ? "Resolving" : "Removing") + " Entity[publicId='" + (publicId != null ? publicId : "") + "', systemId='" + (systemId != null ? systemId : "") + "']");

      if (allowEntities && parent != null) {
         return parent.resolveEntity(publicId, systemId);
      }

      return allowEntities ? null : new InputSource(new ByteArrayInputStream("".getBytes(CharacterSets.UTF_8)));
   }
}
