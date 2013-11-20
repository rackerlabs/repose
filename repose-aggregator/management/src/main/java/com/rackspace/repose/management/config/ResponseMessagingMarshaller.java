package com.rackspace.repose.management.config;

import com.rackspace.papi.service.rms.config.ObjectFactory;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 30, 2012
 * Time: 2:09:42 PM
 */
public class ResponseMessagingMarshaller implements ReposeMarshaller {

   private static final Logger LOG = LoggerFactory.getLogger(SystemModelMarshaller.class);

   private final JAXBContext jaxbContext = JAXBContext.newInstance(ReposeConfiguration.RMS.getConfigContextPath());
   private final Marshaller marshaller = jaxbContext.createMarshaller();
   private final ObjectFactory objectFactory = new ObjectFactory();

   public ResponseMessagingMarshaller() throws JAXBException {
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
   }

   @Override
   public void marshal(String configurationRoot, Object config) throws FileNotFoundException, JAXBException {
      if (!(config instanceof ResponseMessagingConfiguration)) {
         // TODO: Clean up exception handling
         throw new IllegalArgumentException("The config object passed is not a ResponseMessagingConfiguration.");
      }

      marshaller.marshal(objectFactory.createResponseMessaging((ResponseMessagingConfiguration) config), new FileOutputStream(configurationRoot + ReposeConfiguration.RMS.getConfigFilename()));

      LOG.info("Created " + ReposeConfiguration.RMS.getConfigFilename() + " : " + config.toString());
   }

   @Override
   public JAXBElement<?> unmarshal(String configurationRoot) throws FileNotFoundException, JAXBException {
      return (JAXBElement<ResponseMessagingConfiguration>) jaxbContext.createUnmarshaller().unmarshal(new File(configurationRoot + ReposeConfiguration.RMS.getConfigFilename()));
   }
}
