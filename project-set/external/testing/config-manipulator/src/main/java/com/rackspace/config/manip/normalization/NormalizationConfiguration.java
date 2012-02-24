package com.rackspace.config.manip.normalization;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;

import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig;

/**
 * @author fran
 */
@Path("/normalization")
public class NormalizationConfiguration {
   private static final String NORMALIZATION_CONFIG_FILE_NAME = "content-normalization.cfg.xml";
   private static final String NORMALIZATION_CONFIG_FILE_PATH = "/etc/powerapi/";
   private JAXBContext jaxbContext = null;
   private Marshaller marshaller = null;

   @POST
   @Consumes("application/xml")
   public Response create(ContentNormalizationConfig config) {
      try {

         if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance("com.rackspace.papi.components.normalization.config");
            marshaller = jaxbContext.createMarshaller();
         }

         marshaller.marshal(new JAXBElement(new QName("uri", "local"), ContentNormalizationConfig.class, config),
                 new FileOutputStream(NORMALIZATION_CONFIG_FILE_PATH + NORMALIZATION_CONFIG_FILE_NAME));
      } catch (JAXBException e) {
         e.printStackTrace();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }

      System.out.println("Created content-normalization.cfg.xml file: " + config.toString());

      return Response.created(URI.create("/normalization/")).build();
   }
}
