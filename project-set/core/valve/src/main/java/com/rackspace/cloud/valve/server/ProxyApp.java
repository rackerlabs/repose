package com.rackspace.cloud.valve.server;

import com.rackspace.cloud.valve.logging.DefaultLogConfigurator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zinic
 */
public class ProxyApp {

   private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);
   private static final String DEFAULT_CFG_DIR = "/etc/repose";

   public static void main(String[] args) throws Exception {

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

      validateConfigDirectory(commandLineArgs);

      final PowerApiValveServerControl serverControl = new PowerApiValveServerControl(commandLineArgs);

      if (commandLineArgs.getAction().equalsIgnoreCase(CommandLineArguments.ACTION_START)) {
         serverControl.startPowerApiValve();
      }
      if (commandLineArgs.getAction().equalsIgnoreCase(CommandLineArguments.ACTION_STOP)) {
         serverControl.stopPowerApiValve();
      }
   }

   private static boolean validPorts(CommandLineArguments commandLineArgs) {
      boolean valid = true;

      if ((!(portIsInRange(commandLineArgs.getHttpPort())))) {
         LOG.info("Invalid Repose http port, use a value between 1024 and 49150");
         valid = false;
      }

      if ((!(portIsInRange(commandLineArgs.getStopPort())))) {
         LOG.info("Invalid Repose stop port, use a value between 1024 and 49150");
         valid = false;
      }

      Integer httpsPort = commandLineArgs.getHttpsPort();
      if (httpsPort != null) {
         if (!portIsInRange(httpsPort)) {
            LOG.info("Invalid Repose https port, use a value between 1024 and 49150");
            valid = false;
         }
      }

      return valid;
   }

   private static void validateConfigDirectory(CommandLineArguments commandLineArgs) {
      if (commandLineArgs.getConfigDirectory() == null || commandLineArgs.getConfigDirectory().length() <= 0) {
         commandLineArgs.setConfigDirectory(DEFAULT_CFG_DIR);
      }
   }

   private static void displayUsage(CmdLineParser cmdLineParser, Exception e) {
      System.err.println(e.getMessage());
      System.err.println("java -jar repose-valve.jar [options...] arguments...");
      cmdLineParser.printUsage(System.err);
   }

   private static boolean portIsInRange(int portNum) {
      if ((portNum < 49150) && (portNum > 1024)) {
         return true;
      }
      return false;
   }

}
