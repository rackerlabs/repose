package com.rackspace.cloud.valve.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.OutputStream;

import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerApiValveServerControl {

   private static final Logger LOG = LoggerFactory.getLogger(PowerApiValveServerControl.class);
   private static final String LOCALHOST_IP = "127.0.0.1";
   private Server serverInstance;
   private CommandLineArguments commandLineArgs;

   public PowerApiValveServerControl(CommandLineArguments commandLineArgs) {
      this.commandLineArgs = commandLineArgs;
   }

   public void startPowerApiValve() throws Exception {
      
      try {
         final Integer httpPort = commandLineArgs.getHttpPort();
         final Integer httpsPort = commandLineArgs.getHttpsPort();

         serverInstance = new ValveJettyServerBuilder(httpPort, httpsPort, commandLineArgs.getConfigDirectory()).newServer();
         serverInstance.setStopAtShutdown(true);
         serverInstance.start();
         final Thread monitor = new MonitorThread(serverInstance, commandLineArgs.getStopPort(), LOCALHOST_IP);
         monitor.start();

         LOG.info("Repose running and listening on http port: " + httpPort);
      } catch (Exception e) {
         LOG.error("Repose could not be started. Reason: " + e.getMessage(), e);
         LOG.error("Repose will now stop.");

         if (serverInstance != null) {
            serverInstance.stop();
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
