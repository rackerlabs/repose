package com.rackspace.cloud.valve.server;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;

/**
 * @author zinic
 */
public final class ProxyApp {

   private static final String DEFAULT_CFG_DIR = "/etc/repose";
   private static final int UPPER_PORT = 49150;
   private static final int LOWER_PORT = 1024;

   public static void main(String[] args) throws IOException {
      final CommandLineArguments commandLineArgs = new CommandLineArguments();
      final CmdLineParser cmdLineParser = new CmdLineParser(commandLineArgs);

      try {
         cmdLineParser.parseArgument(args);
       } catch (CmdLineException e) {
           
         displayUsage(null, cmdLineParser, e);
         return;
      }

      if (!validPorts(commandLineArgs)) {
         return;
      }
      
      try{
        validateConfigDirectory(commandLineArgs);
      }catch(IOException e){
        displayUsage("Unable to validate config directory", cmdLineParser, e);
        return;
      }

      final PowerApiValveServerControl serverControl = new PowerApiValveServerControl(
              commandLineArgs.getHttpPort(),
              commandLineArgs.getHttpsPort(),
              commandLineArgs.getConfigDirectory(),
              commandLineArgs.getInsecure());

       serverControl.startPowerApiValve();
   }
   
   private ProxyApp(){
   }


   @SuppressWarnings("PMD.SystemPrintln")
   private static boolean validPorts(CommandLineArguments commandLineArgs) {
      boolean valid = true;

      Integer httpPort = commandLineArgs.getHttpPort();
      if ((httpPort != null) && (!(portIsInRange(httpPort)))) {
         System.err.println("Invalid Repose http port, use a value between 1024 and 49150");
         valid = false;
      }

      Integer httpsPort = commandLineArgs.getHttpsPort();
      if (httpsPort != null && !portIsInRange(httpsPort)) {
         System.err.println("Invalid Repose https port, use a value between 1024 and 49150");
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
      // IF there is a usable Log4J Properties file in the configuration directory,
      // THEN add it to the System properties.
      File log4jProps = new File(commandLineArgs.getConfigDirectory() + "/log4j.properties");
      if(log4jProps.exists() && log4jProps.isFile() && log4jProps.canRead()){
         System.getProperties().setProperty("log4j.configuration", log4jProps.toURI().toASCIIString());
      }
   }
   
   @SuppressWarnings("PMD.SystemPrintln")
   private static void displayUsage(String msg, CmdLineParser cmdLineParser, Exception e) {
      if(msg != null && msg.length() > 0) {
          System.err.println(msg);
      }
      System.err.println(e.getMessage());
      System.err.println("java -jar repose-valve.jar [options...]");
      cmdLineParser.printUsage(System.err);
   }

   private static boolean portIsInRange(int portNum) {
      return (portNum < UPPER_PORT) && (portNum > LOWER_PORT);
   }
}
