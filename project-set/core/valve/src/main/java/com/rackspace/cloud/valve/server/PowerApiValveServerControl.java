package com.rackspace.cloud.valve.server;

import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.parser.ConfigurationParserFactory;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.commons.config.resource.impl.BufferedURLConfigurationResource;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.SslConfiguration;
import com.rackspace.papi.domain.Port;
import java.io.File;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PowerApiValveServerControl {

   private static final Logger LOG = LoggerFactory.getLogger(PowerApiValveServerControl.class);
   private static final String LOCALHOST_IP = "127.0.0.1";
   private CommandLineArguments commandLineArgs;
   private final List<Port> ports = new ArrayList<Port>();

   public PowerApiValveServerControl(CommandLineArguments commandLineArgs) {
      this.commandLineArgs = commandLineArgs;

      if (commandLineArgs.getHttpPort() != null) {
         ports.add(new Port("http", commandLineArgs.getHttpPort()));
      }

      if (commandLineArgs.getHttpsPort() != null) {
         ports.add(new Port("https", commandLineArgs.getHttpsPort()));
      }
   }

   private SslConfiguration validateSsl() throws MalformedURLException {
      SslConfiguration sslConfiguration = null;

      if (commandLineArgs.getHttpsPort() != null) {

         sslConfiguration = readSslConfiguration(commandLineArgs.getConfigDirectory());

         if (sslConfiguration == null) {
            throw new ConfigurationResourceException("Repose is configured to run on https but the ssl configuration is not in container.cfg.xml.");
         }

         if (sslConfiguration.getKeystoreFilename() == null) {
            throw new ConfigurationResourceException("Repose is configured to run on https but the ssl keystore filename is not in container.cfg.xml.");
         }

         if (sslConfiguration.getKeystorePassword() == null) {
            throw new ConfigurationResourceException("Repose is configured to run on https but the ssl keystore password is not in container.cfg.xml.");
         }

         if (sslConfiguration.getKeyPassword() == null) {
            throw new ConfigurationResourceException("Repose is configured to run on https but the ssl key password is not in container.cfg.xml.");
         }
      }

      return sslConfiguration;
   }

   private SslConfiguration readSslConfiguration(String cfgRoot) throws MalformedURLException {
      final URL configurationLocation = new URL("file://" + cfgRoot + File.separator + "c.cfg.xml");
      final JaxbConfigurationParser<ContainerConfiguration> containerConfigParser = ConfigurationParserFactory.getXmlConfigurationParser(ContainerConfiguration.class,null );
      final ContainerConfiguration cfg = containerConfigParser.read(new BufferedURLConfigurationResource(configurationLocation));

      if (cfg != null && cfg.getDeploymentConfig() != null) {
         return cfg.getDeploymentConfig().getSslConfiguration();
      }

      throw new ConfigurationResourceException("Container configuration is not valid. Please check your configuration.");
   }

   public void startPowerApiValve() {
      Server serverInstance = null;
      
      try {

         serverInstance = new ValveJettyServerBuilder(commandLineArgs.getConfigDirectory(), ports, validateSsl(), commandLineArgs.getConnectionFramework(), commandLineArgs.getInsecure()).newServer();
         serverInstance.setStopAtShutdown(true);
         serverInstance.start();
         final Thread monitor = new MonitorThread(serverInstance, commandLineArgs.getStopPort(), LOCALHOST_IP);
         monitor.start();

         for (Port p : ports) {
            if (p != null) {
               LOG.info("Repose running and listening on " + p.getProtocol().toLowerCase() + " port: " + p.getPort());   
            }
         }

      } catch (Exception e) {
         LOG.error("Repose could not be started. Reason: " + e.getMessage(), e);
         LOG.error("Repose will now stop.");

         if (serverInstance != null) {
             try {
                serverInstance.stop();
             } catch(Exception ex) {
                 LOG.error("Error stopping server", ex);
             }
         }
      }
   }

   public void stopPowerApiValve() {
      try {
         final Socket s = new Socket(InetAddress.getByName(LOCALHOST_IP), commandLineArgs.getStopPort());
         final OutputStream out = s.getOutputStream();

         LOG.info("Sending Repose stop request");

         out.write(("\r\n").getBytes());
         out.flush();
         s.close();
      } catch (IOException ioex) {
         LOG.error("An error occurred while attempting to stop Repose. Reason: " + ioex.getMessage());
      }
   }
}
