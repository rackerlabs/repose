package com.rackspace.cloud.valve.server;

import com.rackspace.papi.service.logging.common.log4jconf.Log4jAppender;
import com.rackspace.papi.service.logging.common.log4jconf.Log4jPropertiesBuilder;
import org.apache.log4j.PropertyConfigurator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class ProxyApp {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);
    private static final String DEFAULT_CFG_DIR = "/etc/powerapi";
    private static Log4jAppender defaultLog4j = new Log4jAppender("consoleOut", "ConsoleAppender", "PatternLayout", "%d %-4r [%t] %-5p %c %x - %m%n");
    private static Log4jPropertiesBuilder log4jPropertiesBuilder = new Log4jPropertiesBuilder();

    public static void main(String[] args) throws Exception {
        // Use default logging confg which sets to DEBUG
        //BasicConfigurator.configure();
        log4jPropertiesBuilder.addLog4jAppender(defaultLog4j);
        PropertyConfigurator.configure(log4jPropertiesBuilder.getLoggingConfig());


        // Turn off Jetty logging
        org.apache.log4j.Logger.getLogger("org.eclipse.jetty").setLevel(org.apache.log4j.Level.OFF);

        final CommandLineArguments commandLineArgs = new CommandLineArguments();
        final CmdLineParser cmdLineParser = new CmdLineParser(commandLineArgs);

        try {
            cmdLineParser.parseArgument(args);
        } catch (CmdLineException e) {
            displayUsage(cmdLineParser, e);
            return;
        }

        if ((!(portIsInRange(commandLineArgs.getPort()))) || (!(portIsInRange(commandLineArgs.getStopPort())))) {
            LOG.info("Invalid Power API Valve port setting, use a value between 1024 and 49150");
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

    private static void validateConfigDirectory(CommandLineArguments commandLineArgs) {
        if (commandLineArgs.getConfigDirectory() == null || commandLineArgs.getConfigDirectory().length() <= 0) {
            commandLineArgs.setConfigDirectory(DEFAULT_CFG_DIR);
        }
    }

    private static void displayUsage(CmdLineParser cmdLineParser, Exception e) {
        System.err.println(e.getMessage());
        System.err.println("java -jar PowerApiServer.jar [options...] arguments...");
        cmdLineParser.printUsage(System.err);
    }

    private static boolean portIsInRange(int portNum) {
        if ((portNum < 49150) && (portNum > 1024)) {
            return true;
        }
        return false;
    }
}
