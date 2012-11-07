package com.rackspace.repose.management.config;

import com.rackspace.papi.model.ObjectFactory;
import com.rackspace.papi.model.SystemModel;
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
 * Time: 2:08:51 PM
 */
public class SystemModelMarshaller implements ReposeMarshaller {

   private static final Logger LOG = LoggerFactory.getLogger(SystemModelMarshaller.class);

   private final JAXBContext jaxbContext = JAXBContext.newInstance(ReposeConfiguration.SYSTEM.getConfigContextPath());
   private final Marshaller marshaller = jaxbContext.createMarshaller();
   private final ObjectFactory objectFactory = new ObjectFactory();

   public SystemModelMarshaller() throws JAXBException {
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
   }

   @Override
   public void marshal(String configurationRoot, Object config) throws FileNotFoundException, JAXBException {
      if (!(config instanceof SystemModel)) {
         // TODO: Clean up exception handling
         throw new IllegalArgumentException("The config object passed is not a SystemModel.");
      }

      marshaller.marshal(objectFactory.createSystemModel((SystemModel) config), new FileOutputStream(configurationRoot + ReposeConfiguration.SYSTEM.getConfigFilename()));

      LOG.info("Created " + ReposeConfiguration.SYSTEM.getConfigFilename() + " : " + config.toString());
   }

   @Override
   public JAXBElement<?> unmarshal(String configurationRoot) throws FileNotFoundException, JAXBException {
      return (JAXBElement<SystemModel>) jaxbContext.createUnmarshaller().unmarshal(new File(configurationRoot + ReposeConfiguration.SYSTEM.getConfigFilename()));
   }
}
