package org.openrepose.core.valve;

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;

public final class ProxyApp {

   private static final String DEFAULT_CFG_DIR = "/etc/repose";

   public static void main(String[] args) throws IOException {
      final CommandLineArguments commandLineArgs = new CommandLineArguments();
      final CmdLineParser cmdLineParser = new CmdLineParser(commandLineArgs);

      try {
         cmdLineParser.parseArgument(args);
       } catch (CmdLineException e) {
         displayUsage(null, cmdLineParser, e);
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


   private static void validateConfigDirectory(CommandLineArguments commandLineArgs) throws IOException {
      if (commandLineArgs.getConfigDirectory() == null || commandLineArgs.getConfigDirectory().length() <= 0) {
         commandLineArgs.setConfigDirectory(DEFAULT_CFG_DIR);
      } else {
         File file = new File(commandLineArgs.getConfigDirectory());
         commandLineArgs.setConfigDirectory(file.getCanonicalPath());
      }
      // IF there is a usable Log4J Properties file in the configuration directory,
      // THEN add it to the System properties.
      File log4jProps = new File(commandLineArgs.getConfigDirectory() + "/log4j2-test.xml");
      if(log4jProps.exists() && log4jProps.isFile() && log4jProps.canRead()){
         System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, log4jProps.toURI().toASCIIString());
      } else {
         log4jProps = new File(commandLineArgs.getConfigDirectory() + "/log4j2.xml");
         if(log4jProps.exists() && log4jProps.isFile() && log4jProps.canRead()){
            System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, log4jProps.toURI().toASCIIString());
         }
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
}
