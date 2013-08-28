package com.rackspace.papi.test;


import org.glassfish.embeddable.*;
import org.glassfish.internal.embedded.EmbeddedFileSystem;
import org.glassfish.internal.embedded.Server;

import java.io.File;
import java.io.IOException;

import static org.glassfish.embeddable.GlassFishRuntime.bootstrap;

/**
 * Create an embedded multinode server
 */
public class ReposeGlassfishServer {

    static GlassFish glassfish;
    static Server server;
    static String installRoot = "/Users/lisa.clark/workspace/repose/test/spock-functional-test/target/glassfish1";
    static String reposeRootWar = "/Users/lisa.clark/workspace/repose/test/spock-functional-test/target/repose_home/ROOT.war";
    static int httpPort = 9009;

    public static void main(String[] args) throws GlassFishException {

        //TODO: grab arguments and don't use my hardcoded values
        for (String arg : args) {
            // grab install root
        }

        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
        GlassFishProperties properties = new GlassFishProperties();

        glassfish = runtime.newGlassFish(properties);
        glassfish.start();

        File war = new File(reposeRootWar);
        Deployer deployer = glassfish.getDeployer();
        deployer.deploy(war, "--name=repose", "--contextroot=/", "--force=true");
    }

}
