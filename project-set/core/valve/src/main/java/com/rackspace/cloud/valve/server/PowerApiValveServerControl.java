package com.rackspace.cloud.valve.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.OutputStream;

import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.parser.ConfigurationParserFactory;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.commons.config.resource.impl.BufferedURLConfigurationResource;
import com.rackspace.papi.container.config.ContainerConfiguration;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerApiValveServerControl {

   private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);
   private static final String LOCALHOST_IP = "127.0.0.1";
   private Server serverInstance;
   private CommandLineArguments commandLineArgs;

   public PowerApiValveServerControl(CommandLineArguments commandLineArgs) {
      this.commandLineArgs = commandLineArgs;
   }

   public int readPortNumberFromConfiguration(String cfgRoot) throws MalformedURLException, ConfigurationResourceException {
      final URL configurationLocation = new URL("file://" + cfgRoot + "/container.cfg.xml");
      final JaxbConfigurationParser<ContainerConfiguration> containerConfigParser = ConfigurationParserFactory.getXmlConfigurationParser(ContainerConfiguration.class);
      final ContainerConfiguration cfg = containerConfigParser.read(new BufferedURLConfigurationResource(configurationLocation));

      if (cfg != null && cfg.getDeploymentConfig() != null && cfg.getDeploymentConfig().getPort() != null) {
         return cfg.getDeploymentConfig().getPort();
      }

      throw new ConfigurationResourceException("Container configuration is not valid. Please check your configuration.");
   }

   public void startPowerApiValve() throws Exception {
      final Thread monitor = new MonitorThread(serverInstance, commandLineArgs.getStopPort(), LOCALHOST_IP);
      monitor.start();

      try {
         final int startPort = commandLineArgs.getPort() != null ? commandLineArgs.getPort() : readPortNumberFromConfiguration(commandLineArgs.getConfigDirectory());

         serverInstance = new ValveJettyServerBuilder(startPort, commandLineArgs.getConfigDirectory()).newServer();
         serverInstance.setStopAtShutdown(true);
         serverInstance.start();

         LOG.info("Power API Valve running and listening on port: " + startPort);
      } catch (Exception e) {
         LOG.error("Power API Valve could not be started. Reason: " + e.getMessage(), e);
         LOG.error("Power API Valve will now stop.");

         monitor.interrupt();

         if (serverInstance != null) {
            serverInstance.stop();
         }
      }
   }

   public void stopPowerApiValve() {
      try {
         final Socket s = new Socket(InetAddress.getByName(LOCALHOST_IP), commandLineArgs.getStopPort());
         final OutputStream out = s.getOutputStream();

         LOG.info("Sending Power API Valve stop request");

         out.write(("\r\n").getBytes());
         out.flush();
         s.close();
      } catch (IOException ioex) {
         LOG.error("An error occured while attempting to stop Power API Valve. Reason: " + ioex.getMessage());
      }
   }
}
