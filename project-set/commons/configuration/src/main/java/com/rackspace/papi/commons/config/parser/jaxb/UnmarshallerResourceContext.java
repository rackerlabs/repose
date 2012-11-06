package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;

public class UnmarshallerResourceContext implements ResourceContext<Unmarshaller, Object> {

   private final ConfigurationResource cfgResource;

   public UnmarshallerResourceContext(ConfigurationResource cfgResource) {
      this.cfgResource = cfgResource;
   }

   @Override
   public Object perform(Unmarshaller resource) throws ResourceContextException {
      try {
         return resource.unmarshal(cfgResource.newInputStream());
      } catch (JAXBException jaxbe) {
         throw new ResourceContextException("Failed to unmarshall resource " + cfgResource.name()
                 + " - Error code: "
                 + jaxbe.getErrorCode()
                 + " - Reason: "
                 + jaxbe.getMessage(), jaxbe.getLinkedException());
      } catch (IOException ioe) {
         throw new ResourceContextException("An I/O error has occured while trying to read resource " + cfgResource.name() + " - Reason: " + ioe.getMessage(), ioe);
      } catch (Exception ex) {
         throw new ResourceContextException("Failed to unmarshall resource " + cfgResource.name() + " - Reason: " + ex.getMessage(), ex);
      }
   }
}
