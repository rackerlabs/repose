package com.rackspace.cloud.valve.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorThread extends Thread {

   private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);
   private ServerSocket socket;
   private Server serverInstance;
   private final String MONITOR_NAME = "StopMonitor";

   public MonitorThread(Server serverInstance, final int stopPort, final String ipAddress) {
      if (serverInstance == null) {
         throw new RuntimeException("The Jetty server instance is null.  The Repose stop Monitor can not be initialized.");
      }

      this.serverInstance = serverInstance;

      setDaemon(true);
      setName(MONITOR_NAME);

      try {
         socket = new ServerSocket(stopPort, 1, InetAddress.getByName(ipAddress));
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void run() {
      Socket accept;

      try {
         accept = socket.accept();
         BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
         reader.readLine();
         LOG.info("Stopping Power API Valve...");
         serverInstance.stop();
         LOG.info("Power API Valve has been stopped");
         accept.close();
         socket.close();
      } catch (InterruptedException ie) {
         //NOP - normal exit
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}
