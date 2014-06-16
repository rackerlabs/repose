package com.rackspace.papi.test;

import org.apache.commons.cli.*;

public class ReposeContainerUtil {

    private ReposeContainerUtil(){
    }

    public static ReposeContainerProps parseArgs(String[] args) throws ParseException {

        Options options = new Options();
        CommandLineParser parser = new BasicParser();

        Option portOpt = new Option("p", true, "Repose port to listen on");
        Option rootwarOpt = new Option("w", true, "Location of ROOT.war");
        Option applicationWarsOpt = new Option("war", true, "");

        portOpt.setRequired(true);
        rootwarOpt.setRequired(true);
        options.addOption(portOpt).addOption(rootwarOpt).addOption(applicationWarsOpt);
        final CommandLine cmdline;

        cmdline = parser.parse(options, args);

        return new ReposeContainerProps(cmdline.getOptionValue("p"), cmdline.getOptionValue("w"), cmdline.getOptionValues("war"));
    }
}
