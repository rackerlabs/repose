package com.rackspace.cloud.valve.server;

import com.rackspace.cloud.valve.logging.DefaultLogConfigurator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.OptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zinic
 */
public final class ProxyApp {

   private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);
   private static final String DEFAULT_CFG_DIR = "/etc/repose";
   private static final int UPPER_PORT = 49150;
   private static final int LOWER_PORT = 1024;

   public static void main(String[] args) throws IOException {

      DefaultLogConfigurator.configure();

      final CommandLineArguments commandLineArgs = new CommandLineArguments();
      final CmdLineParser cmdLineParser = new CmdLineParser(commandLineArgs);

      try {
         cmdLineParser.parseArgument(args);
       } catch (CmdLineException e) {
           
         displayUsage(cmdLineParser, e);
         return;
      }

      if (!validPorts(commandLineArgs)) {
         return;
      }
      
      try{
        validateConfigDirectory(commandLineArgs);
      }catch(IOException e){
        System.err.println(e.getMessage());
        cmdLineParser.printUsage(System.err);
        return;
      }

      final PowerApiValveServerControl serverControl = new PowerApiValveServerControl(commandLineArgs);

      if (commandLineArgs.getAction().equalsIgnoreCase(CommandLineArguments.ACTION_START)) {
         serverControl.startPowerApiValve();
      }
      if (commandLineArgs.getAction().equalsIgnoreCase(CommandLineArguments.ACTION_STOP)) {
         serverControl.stopPowerApiValve();
      }
   }
   
   private ProxyApp(){
   }


   private static boolean validPorts(CommandLineArguments commandLineArgs) {
      boolean valid = true;

      Integer httpPort = commandLineArgs.getHttpPort();
      if ((httpPort != null) && (!(portIsInRange(httpPort)))) {
         LOG.info("Invalid Repose http port, use a value between 1024 and 49150");
         valid = false;
      }

      Integer httpsPort = commandLineArgs.getHttpsPort();
      if (httpsPort != null && !portIsInRange(httpsPort)) {
         LOG.info("Invalid Repose https port, use a value between 1024 and 49150");
         valid = false;
      }

      if ((!(portIsInRange(commandLineArgs.getStopPort())))) {
         LOG.info("Invalid Repose stop port, use a value between 1024 and 49150");
         valid = false;
      }

      return valid;
   }

   private static void validateConfigDirectory(CommandLineArguments commandLineArgs) throws IOException {
      if (commandLineArgs.getConfigDirectory() == null || commandLineArgs.getConfigDirectory().length() <= 0) {
         commandLineArgs.setConfigDirectory(DEFAULT_CFG_DIR);
      } else {
         File file = new File(commandLineArgs.getConfigDirectory());
         commandLineArgs.setConfigDirectory(file.getCanonicalPath());
      }
   }
   
   @SuppressWarnings("PMD.SystemPrintln")
   private static void displayUsage(CmdLineParser cmdLineParser, Exception e) {
      System.err.println(e.getMessage());
      System.err.println("java -jar repose-valve.jar [options...] arguments...");
      cmdLineParser.printUsage(System.err);
   }

   private static boolean portIsInRange(int portNum) {
      return ((portNum < UPPER_PORT) && (portNum > LOWER_PORT));
   }
}
