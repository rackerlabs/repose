package org.openrepose.commons.config.parser.jaxb;

import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.pooling.ResourceContext;
import org.openrepose.commons.utils.pooling.ResourceContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;

/**
 * Uses {@link org.openrepose.commons.config.parser.jaxb.UnmarshallerValidator UnmarshallerValidator} to validate
 * and then unmarshall the given {@link org.openrepose.commons.config.resource.ConfigurationResource ConfigurationResource}.
 */
public class UnmarshallerResourceContext implements ResourceContext<UnmarshallerValidator, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(UnmarshallerResourceContext.class);
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
          LOG.trace("failed to unmarshall", jaxbe);
         throw new ResourceContextException("Failed to unmarshall resource " + cfgResource.name()+ " - "+jaxbe.getCause()
                 + " - Error code: "
                 + jaxbe.getErrorCode()
                 + " - Reason: "
                 + jaxbe.getMessage(), jaxbe.getLinkedException());
      } catch (IOException ioe) {
         throw new ResourceContextException("An I/O error has occured while trying to read resource " + cfgResource.name() + " - Reason: " + ioe.getMessage(), ioe);
      } catch (SAXException se ) {
          throw new ResourceContextException( "Validation error on resource " + cfgResource.name() + " - " + se.getMessage(), se );
      } catch (Exception ex) {
         throw new ResourceContextException("Failed to unmarshall resource " + cfgResource.name() + " - Reason: " + ex.getMessage(), ex);
      }
   }
}
