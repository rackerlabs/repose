package com.rackspace.cloud.valve.logging;

import com.rackspace.papi.service.logging.common.log4jconf.Log4jAppender;
import com.rackspace.papi.service.logging.common.log4jconf.Log4jPropertiesBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;

public class DefaultLogConfigurator {

   private static final String DEFAULT_LOG_DIR = "/var/log/repose/current.log";
   private static final String DEFAULT_LAYOUT = "PatternLayout";
   private static final String DEFAULT_LOG_FRMT = "%d %-4r [%t] %-5p %c %x - %m%n";

   private static Log4jAppender consoleAppender = new Log4jAppender("consoleOut", "ConsoleAppender", DEFAULT_LAYOUT, DEFAULT_LOG_FRMT);
   private static Log4jAppender fileAppender = new Log4jAppender("defaultFile", "RollingFileAppender", DEFAULT_LAYOUT, DEFAULT_LOG_FRMT);
   private static Log4jPropertiesBuilder log4jPropertiesBuilder = new Log4jPropertiesBuilder();

   public static void configure() {

        fileAppender.addProp("MaxFileSize", "2MB");
        fileAppender.addProp("MaxBackupIndex", "2");
        fileAppender.addProp("File", DEFAULT_LOG_DIR);
        log4jPropertiesBuilder.addLog4jAppender(consoleAppender);
        log4jPropertiesBuilder.addLog4jAppender(fileAppender);
        PropertyConfigurator.configure(log4jPropertiesBuilder.getLoggingConfig());

        // Turn off default Jetty and Jersey logging
        org.apache.log4j.Logger.getLogger("org.eclipse.jetty").setLevel(Level.OFF);
        org.apache.log4j.Logger.getLogger("com.sun.jersey").setLevel(Level.OFF);
   }
}
