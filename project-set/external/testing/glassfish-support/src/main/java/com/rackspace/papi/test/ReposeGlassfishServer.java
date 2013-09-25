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
    static String reposeRootWar = "/Volumes/workspace/repose/test/spock-functional-test/target/repose_home/ROOT.war";
    static int reposePort;

    public static void main(String[] args) throws GlassFishException, NamingException, IOException {

        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        Option portOpt = new Option("p", true, "Repose port to listen on");
        Option configOpt = new Option("c", true, "Config directory");
        Option clusterOpt = new Option("l", true, "Cluster name");
        Option nodeOpt = new Option("n", true, "Node name");

        portOpt.setRequired(true);
        clusterOpt.setRequired(true);
        nodeOpt.setRequired(true);

        options.addOption(portOpt).addOption(configOpt).addOption(clusterOpt).addOption(nodeOpt);

        GlassFishProperties properties = new GlassFishProperties();

        final CommandLine cmdline;
        try {
            cmdline = parser.parse(options, args);
            if (cmdline.hasOption("p")) {
                reposePort = Integer.parseInt(cmdline.getOptionValue("p"));
            } else {
                System.err.println("Repose port is required");
                System.exit(-1);
            }
            if (cmdline.hasOption("c")) {
                properties.setProperty("powerapi-config-directory", cmdline.getOptionValue("c"));
            }
            properties.setProperty("repose-cluster-id", cmdline.getOptionValue("l"));
            properties.setProperty("repose-node-id", cmdline.getOptionValue("n"));
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
