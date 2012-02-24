package com.rackspace.config.manip.versioning;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;

/**
 * @author fran
 */
@Path("/versioning")
public class VersioningConfiguration {
   private static final String VERSIONING_CONFIG_FILE_NAME = "versioning.cfg.xml";
   private static final String VERSIONING_CONFIG_FILE_PATH = "/etc/powerapi/";
   private JAXBContext jaxbContext = null;
   private Marshaller marshaller = null;

   @POST
   @Consumes("application/xml")
   public Response create(ServiceVersionMappingList config) {
      try {

         if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance("com.rackspace.papi.components.versioning.config");
            marshaller = jaxbContext.createMarshaller();
         }

         marshaller.marshal(new JAXBElement(new QName("uri", "local"), ServiceVersionMappingList.class, config),
                 new FileOutputStream(VERSIONING_CONFIG_FILE_PATH + VERSIONING_CONFIG_FILE_NAME));
      } catch (JAXBException e) {
         e.printStackTrace();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }

      System.out.println("Created versioning.cfg.xml file: " + config.toString());

      return Response.created(URI.create("/versioning/" + config.getServiceRoot())).build();
   }
}
