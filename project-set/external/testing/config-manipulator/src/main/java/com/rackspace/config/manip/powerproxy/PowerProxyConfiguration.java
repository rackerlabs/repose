package com.rackspace.config.manip.powerproxy;

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

import com.rackspace.config.manip.jmx.Client;
import com.rackspace.papi.model.PowerProxy;

/**
 * @author fran
 */
@Path("/powerproxy")
public class PowerProxyConfiguration {
   private static final String POWER_PROXY_CONFIG_FILE_NAME = "power-proxy.cfg.xml";
   private static final String POWER_PROXY_CONFIG_FILE_PATH = "/etc/powerapi/";
   private JAXBContext jaxbContext = null;
   private Marshaller marshaller = null;
   private final Client jmxClient;

   public PowerProxyConfiguration() {
      this.jmxClient = new Client();
   }

   @POST
   @Consumes("application/xml")
   public Response create(PowerProxy config) {

      PowerProxy returnedPowerProxy = jmxClient.updateReposeSystemConfiguration(config);

      System.out.println("Returned power proxy configuration file: " + returnedPowerProxy.toString());

      return Response.created(URI.create("/powerproxy/" + returnedPowerProxy.getHost())).build();
   }

   private void marshal(PowerProxy config) {
      try {

         if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance("com.rackspace.papi.model");
            marshaller = jaxbContext.createMarshaller();
         }

         marshaller.marshal(new JAXBElement(new QName("uri", "local"), PowerProxy.class, config),
                 new FileOutputStream(POWER_PROXY_CONFIG_FILE_PATH + POWER_PROXY_CONFIG_FILE_NAME));
      } catch (JAXBException e) {
         e.printStackTrace();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }
}
