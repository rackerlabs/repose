package com.rackspace.repose.management.config;

import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.ObjectFactory;
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
 * Time: 1:31:23 PM
 */
public class ContainerMarshaller implements ReposeMarshaller {

   private static final Logger LOG = LoggerFactory.getLogger(ContainerMarshaller.class);

   private final JAXBContext jaxbContext = JAXBContext.newInstance(ReposeConfiguration.CONTAINER.getConfigContextPath());
   private final Marshaller marshaller = jaxbContext.createMarshaller();
   private final ObjectFactory objectFactory = new ObjectFactory();

   public ContainerMarshaller() throws JAXBException {
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
   }

   @Override
   public void marshal(String configurationRoot, Object config) throws FileNotFoundException, JAXBException {
      if (!(config instanceof ContainerConfiguration)) {
         // TODO: Clean up exception handling
         throw new IllegalArgumentException("The config object passed is not a ContainerConfiguration.");
      }

      marshaller.marshal(objectFactory.createReposeContainer((ContainerConfiguration) config), new FileOutputStream(configurationRoot + ReposeConfiguration.CONTAINER.getConfigFilename()));

      LOG.info("Created " + ReposeConfiguration.CONTAINER.getConfigFilename() + " : " + config.toString());
   }

   @Override
   public JAXBElement<?> unmarshal(String configurationRoot) throws FileNotFoundException, JAXBException {
      return (JAXBElement<ContainerConfiguration>) jaxbContext.createUnmarshaller().unmarshal(new File(configurationRoot + ReposeConfiguration.CONTAINER.getConfigFilename()));
   }
}
