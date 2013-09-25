package com.rackspace.papi.test;


import org.apache.commons.cli.*;
import org.glassfish.embeddable.*;

import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;

/**
 * Create an embedded glassfish server
 */
public class ReposeGlassfishServer {

    static GlassFish glassfish;
    static String reposeRootWar;
    static int reposePort;

    public static void main(String[] args) throws GlassFishException, NamingException, IOException {

        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        Option portOpt = new Option("p", true, "Repose port to listen on");
        Option rootwarOpt = new Option("w", true, "Location of ROOT.war");

        portOpt.setRequired(true);
        rootwarOpt.setRequired(true);

        options.addOption(portOpt).addOption(rootwarOpt);

        GlassFishProperties properties = new GlassFishProperties();

        final CommandLine cmdline;
        try {
            cmdline = parser.parse(options, args);
            reposePort = Integer.parseInt(cmdline.getOptionValue("p"));
            reposeRootWar = cmdline.getOptionValue("w");
        } catch (ParseException ex) {
            System.err.println("Failed to start glassfish: " + ex.getMessage());
            System.exit(-1);
        }

        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
        properties.setPort("http-listener", reposePort);

        glassfish = runtime.newGlassFish(properties);
        glassfish.start();

        File war = new File(reposeRootWar);
        Deployer deployer = glassfish.getDeployer();
        deployer.deploy(war, "--name=repose", "--contextroot=/", "--force=true");
    }

}
