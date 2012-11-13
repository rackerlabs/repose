package com.rackspace.cloud.valve.logging;

import com.rackspace.papi.service.logging.common.log4jconf.Log4jAppender;
import com.rackspace.papi.service.logging.common.log4jconf.Log4jPropertiesBuilder;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;

public final class DefaultLogConfigurator {

   private static final String DEFAULT_LOG_DIR = "/var/log/repose/current.log";
   private static final String DEFAULT_LAYOUT = "PatternLayout";
   private static final String DEFAULT_LOG_FRMT = "%d %-4r [%t] %-5p %c %x - %m%n";
   private static final String DEFAULT_LOG_PREFIX = "current";
   private static final String DEFAULT_LOG_SUFFIX = ".log";
   private static Log4jAppender consoleAppender = new Log4jAppender("consoleOut", "ConsoleAppender", DEFAULT_LAYOUT, DEFAULT_LOG_FRMT);
   private static Log4jAppender fileAppender = new Log4jAppender("defaultFile", "RollingFileAppender", DEFAULT_LAYOUT, DEFAULT_LOG_FRMT);
   private static Log4jPropertiesBuilder log4jPropertiesBuilder = new Log4jPropertiesBuilder();

   private DefaultLogConfigurator() {
   }
   
   /**
    * A valid log directory exists and is writable.
    * 
    * @param dir
    * @return 
    */
   private static boolean isValidLogDir(File dir) {
      return dir.exists() && dir.isDirectory() && dir.canWrite();
   }
   
   
   /**
    * Determine if a directory is a "root" directory.
    * 
    * @param dir
    * @return 
    */
   private static boolean isRootDir(File dir) {
      File[] roots = File.listRoots();
      
      for (File root: roots) {
         if(root.equals(dir)) {
            return true;
         }
      }
      
      return false;
   }
   
   /**
    * Append log file name to file path.
    * 
    * @param dir
    * @param defaultValue
    * @return 
    */
   private static String buildLogFilePath(File dir, String defaultValue) {
      if (dir == null) {
         return defaultValue;
      }
      
      String path = dir.getAbsolutePath();
      return dir.getAbsolutePath() + (path.endsWith(File.separator)? "": File.separator) + DEFAULT_LOG_PREFIX + DEFAULT_LOG_SUFFIX;
   }
   
   /**
    * See if the default log dir exists and is writable.  Walk up the 
    * directory tree to find the first dir that is "valid" (exists and is
    * writable).  If none, then use a temp file.
    * 
    * @return 
    */
   @SuppressWarnings("PMD.SystemPrintln")
   private static String determineInitialLogFileName() {
      File defaultLogFile = new File(DEFAULT_LOG_DIR);
      File logDir = defaultLogFile.getParentFile();

      while (logDir != null && !isValidLogDir(logDir)) {
         logDir = logDir.getParentFile();
      }

      String logFile = buildLogFilePath(logDir, DEFAULT_LOG_DIR);
      if (logDir == null || isRootDir(logDir)) {
         try {
            logFile = File.createTempFile(DEFAULT_LOG_PREFIX, DEFAULT_LOG_SUFFIX).getAbsolutePath();
         } catch (IOException ex) {
            System.out.println("Error creating temporary log file.");
         }
      } else {
         String path = logDir.getAbsolutePath();
         logFile = logDir.getAbsolutePath() + (path.endsWith(File.separator)? "": File.separator) + DEFAULT_LOG_PREFIX + DEFAULT_LOG_SUFFIX;
      }

      System.out.println("Initial log file: " + logFile);
      return logFile;
   }

   public static void configure() {
      fileAppender.addProp("MaxFileSize", "2MB");
      fileAppender.addProp("MaxBackupIndex", "2");
      fileAppender.addProp("File", determineInitialLogFileName());
      log4jPropertiesBuilder.addLog4jAppender(consoleAppender);
      log4jPropertiesBuilder.addLog4jAppender(fileAppender);
      PropertyConfigurator.configure(log4jPropertiesBuilder.getLoggingConfig());

      // Turn off default Jetty and Jersey logging
      org.apache.log4j.Logger.getLogger("org.eclipse.jetty").setLevel(Level.OFF);
      org.apache.log4j.Logger.getLogger("com.sun.jersey").setLevel(Level.OFF);
      org.apache.log4j.Logger.getLogger("org.springframework").setLevel(Level.WARN);
   }
}
