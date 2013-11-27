package com.rackspace.cloud.valve.server;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MonitorThread extends Thread {

   private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);
   private ServerSocket socket;
   private Server serverInstance;
   private static final String MONITOR_NAME = "StopMonitor";
   
   public static class MonitorException extends RuntimeException {
      public MonitorException(String message) {
         super(message);
      }
      
      public MonitorException(Throwable cause) {
         super(cause);
      }
   }

   public MonitorThread(Server serverInstance, final int stopPort, final String ipAddress) {
      if (serverInstance == null) {
         throw new MonitorException("The Jetty server instance is null.  The Repose stop Monitor can not be initialized.");
      }

      this.serverInstance = serverInstance;

      setDaemon(true);
      setName(MONITOR_NAME);

      try {
         socket = new ServerSocket(stopPort, 1, InetAddress.getByName(ipAddress));
      } catch (Exception e) {
         throw new MonitorException(e);
      }
   }

   @Override
   public void run() {
      Socket accept;

      try {
         accept = socket.accept();
         BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream(),CharacterSets.UTF_8));
         reader.readLine();
         LOG.info("Stopping Repose...");
         serverInstance.stop();
         LOG.info("Repose has been stopped");
         accept.close();
         socket.close();
      } catch (InterruptedException ie) {
         //NOP - normal exit
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}
