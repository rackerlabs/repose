package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class UnmarshallerResourceContext implements ResourceContext<Unmarshaller, Object> {

   private final ConfigurationResource cfgResource;

   public UnmarshallerResourceContext(ConfigurationResource cfgResource) {
      this.cfgResource = cfgResource;
   }

   //Suppressing the warning as the new exception is using the jaxbe error code and message to pass on to the ResourceContextExcepiton
   @SuppressWarnings("PMD.PreserveStackTrace")
   @Override
   public Object perform(Unmarshaller resource) {
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
