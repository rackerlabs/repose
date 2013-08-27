package com.rackspace.papi.test;


import org.glassfish.embeddable.*;
import org.glassfish.internal.embedded.EmbeddedFileSystem;
import org.glassfish.internal.embedded.Server;

import java.io.File;
import java.io.IOException;

import static org.glassfish.embeddable.GlassFishRuntime.bootstrap;

/**
 * Create an embedded glassfish server
 */
public class ReposeGlassfishServer {

    static GlassFish glassfish;
    static Server server;
    static String installRoot = "/Volumes/workspace/repose/test/spock-functional-test/target/glassfish1";
    static String reposeRootWar = "/Volumes/workspace/repose/test/spock-functional-test/target/ROOT.war";
    static int httpPort = 9009;

    public static void main(String[] args) throws GlassFishException {

        //TODO: grab arguments and don't use my hardcoded values
        for (String arg : args) {
            // grab install root
        }

        // Create glassfish properties
//        BootstrapProperties bootstrapProperties = new BootstrapProperties();
//        bootstrapProperties.setInstallRoot(installRoot);
//        GlassFishRuntime runtime = GlassFishRuntime.bootstrap(bootstrapProperties);

        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
        GlassFishProperties properties = new GlassFishProperties();
        //properties.setInstanceRoot(installRoot + "/domains/domain1");

        glassfish = runtime.newGlassFish(properties);
        glassfish.start();

        File war = new File(reposeRootWar);
        Deployer deployer = glassfish.getDeployer();
        deployer.deploy(war, "--name=repose", "--contextroot=/", "--force=true");
    }

}
