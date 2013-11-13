package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;

/**
 * Uses {@link com.rackspace.papi.commons.config.parser.jaxb.UnmarshallerValidator UnmarshallerValidator} to validate
 * and then unmarshall the given {@link com.rackspace.papi.commons.config.resource.ConfigurationResource ConfigurationResource}.
 */
public class UnmarshallerResourceContext implements ResourceContext<UnmarshallerValidator, Object> {

   private final ConfigurationResource cfgResource;

   public UnmarshallerResourceContext(ConfigurationResource cfgResource) {
      this.cfgResource = cfgResource;
   }

   //Suppressing the warning as the new exception is using the jaxbe error code and message to pass on to the ResourceContextExcepiton
   @SuppressWarnings("PMD.PreserveStackTrace")
   @Override
   public Object perform(UnmarshallerValidator resource) {
      try {

          return resource.validateUnmarshal( cfgResource.newInputStream() );
      } catch (JAXBException jaxbe) {
         throw new ResourceContextException("Failed to unmarshall resource " + cfgResource.name()+ " - "+jaxbe.getCause()
                 + " - Error code: "
                 + jaxbe.getErrorCode()
                 + " - Reason: "
                 + jaxbe.getMessage(), jaxbe.getLinkedException());
      } catch (IOException ioe) {
         throw new ResourceContextException("An I/O error has occured while trying to read resource " + cfgResource.name() + " - Reason: " + ioe.getMessage(), ioe);
      } catch (SAXException se ) {

          throw new ResourceContextException( "Validation error on resource " + cfgResource.name() + " - " + se.getMessage(), se );
      }
        catch (Exception ex) {
         throw new ResourceContextException("Failed to unmarshall resource " + cfgResource.name() + " - Reason: " + ex.getMessage(), ex);
      }
   }
}
