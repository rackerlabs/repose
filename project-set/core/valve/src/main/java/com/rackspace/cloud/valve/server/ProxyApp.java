package com.rackspace.cloud.valve.server;

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

    public static void main(String[] args) throws Exception {
        final CommandLineArguments commandLineArgs = new CommandLineArguments();
        final CmdLineParser cmdLineParser = new CmdLineParser(commandLineArgs);

        try {
            cmdLineParser.parseArgument(args);
        } catch (CmdLineException e) {
            displayUsage(cmdLineParser, e);
            return;
        }

        if( (!(portIsInRange(commandLineArgs.port))) || (!(portIsInRange(commandLineArgs.stopport))) ) {
            LOG.info("Invalid Power API Valve port setting, use a value between 1024 and 49150");
            return;
        }

        validateConfigDirectory(commandLineArgs);

        final PowerApiValveServerControl serverControl = new PowerApiValveServerControl(commandLineArgs);

        if (commandLineArgs.action.equalsIgnoreCase(commandLineArgs.ACTION_START))
            serverControl.startPowerApiValve();
        if (commandLineArgs.action.equalsIgnoreCase(commandLineArgs.ACTION_STOP))
            serverControl.stopPowerApiValve();
    }

    private static void validateConfigDirectory(CommandLineArguments commandLineArgs) {
        if(commandLineArgs.configDirectory == null || commandLineArgs.configDirectory.length() <= 0) {
            commandLineArgs.configDirectory = DEFAULT_CFG_DIR;
        }
    }

    private static void displayUsage(CmdLineParser cmdLineParser, Exception e) {
        System.err.println(e.getMessage());
        System.err.println("java -jar PowerApiServer.jar [options...] arguments...");
        cmdLineParser.printUsage(System.err);
    }

    private static boolean portIsInRange(int portNum) {
        if((portNum < 49150) && (portNum > 1024)) {
            return true;
        }
        return false;
    }
}
