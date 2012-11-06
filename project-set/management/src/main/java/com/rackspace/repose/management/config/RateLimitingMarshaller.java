package com.rackspace.repose.management.config;

import com.rackspace.repose.service.ratelimit.config.ObjectFactory;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;

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
 * Time: 1:58:43 PM
 */
public class RateLimitingMarshaller implements ReposeMarshaller {

   private static final Logger LOG = LoggerFactory.getLogger(RateLimitingMarshaller.class);

   private final JAXBContext jaxbContext = JAXBContext.newInstance(ReposeConfiguration.RATE_LIMITING.getConfigContextPath());
   private final Marshaller marshaller = jaxbContext.createMarshaller();
   private final ObjectFactory objectFactory = new ObjectFactory();

   public RateLimitingMarshaller() throws JAXBException {
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
   }

   @Override
   public void marshal(String configurationRoot, Object config) throws FileNotFoundException, JAXBException {

      if (!(config instanceof RateLimitingConfiguration)) {
         // TODO: Clean up exception handling
         throw new IllegalArgumentException("The config object passed is not a RateLimitingConfiguration.");
      }

      marshaller.marshal(objectFactory.createRateLimiting((RateLimitingConfiguration) config), new FileOutputStream(configurationRoot + ReposeConfiguration.RATE_LIMITING.getConfigFilename()));

      LOG.info("Created " + ReposeConfiguration.RATE_LIMITING.getConfigFilename() + " : " + config.toString());
   }

   @Override
   public JAXBElement<RateLimitingConfiguration> unmarshal(String configurationRoot) throws JAXBException, FileNotFoundException {
      return (JAXBElement<RateLimitingConfiguration>) jaxbContext.createUnmarshaller().unmarshal(new File(configurationRoot + ReposeConfiguration.RATE_LIMITING.getConfigFilename()));
   }
}
