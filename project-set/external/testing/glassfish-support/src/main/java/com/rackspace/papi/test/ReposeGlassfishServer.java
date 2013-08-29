package com.rackspace.papi.test;


import org.glassfish.embeddable.*;
import org.glassfish.embeddable.archive.ScatteredArchive;

import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;

/**
 * Create an embedded glassfish server
 */
public class ReposeGlassfishServer {

    static GlassFish glassfish;
    static String reposeRootWar = "/Volumes/workspace/repose/test/spock-functional-test/target/repose_home/ROOT.war";
    static int reposePort = 9898;

    public static void main(String[] args) throws GlassFishException, NamingException, IOException {

//        File installDir = new File("/Volumes/workspace/repose/test/spock-functional-test/target/repose_home/glassfish");
//        File domainDir = new File(installDir, "domains/domain1");
//
//        File domainConfig = new File(domainDir, "config");
//        File domainXml = new File(domainConfig, "domain.xml");
//
//
//        Server.Builder builder = new Server.Builder("reposeglassfish");
//        EmbeddedFileSystem.Builder efsb = new EmbeddedFileSystem.Builder();
//        efsb.installRoot(installDir);
//        efsb.instanceRoot(domainDir);
//        efsb.autoDelete(true);
//        //efsb.configurationFile(domainXml);
//        EmbeddedFileSystem efs = efsb.build();
//        builder.embeddedFileSystem(efs);
//
//        Server server = builder.build();

        //TODO: grab arguments and don't use my hardcoded values
        for (String arg : args) {
            // grab install root
        }
//
//        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
//        GlassFishProperties properties = new GlassFishProperties();
//
//        glassfish = runtime.newGlassFish(properties);
//        glassfish.start();
//
//        ParameterMap props = new ParameterMap();
//        props.add("powerapi-config-directory", "/Users/lisa/workspace/repose/test/spock-functional-test/target/repose_home/configs");
//
//        String cmd = "set-web-context-param";
////        CommandRunner commandRunner = glassfish.getCommandRunner();
////        CommandResult commandResult = commandRunner.run(cmd, "--name=powerapi-config-directory", "--value=/Users/lisa/workspace/repose/test/spock-functional-test/target/repose_home/configs");
//
////        System.out.println(commandResult);
//
//        File war = new File(reposeRootWar);
//        EmbeddedDeployer deployer = server.getDeployer();
//        DeployCommandParameters commandParameters = new DeployCommandParameters();
//        commandParameters.force = true;
//        commandParameters.name = "repose";
//        commandParameters.contextroot = "/";
////        commandParameters.properties.setProperty("powerapi-config-directory", "/Users/lisa/workspace/repose/test/spock-functional-test/target/repose_home/configs");
//
//        deployer.deploy(war, commandParameters);
//
//

        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();

        GlassFishProperties properties = new GlassFishProperties();

//        File domainXml = new File("/Users/lisa/workspace/repose/test/spock-functional-test/target/repose_home/configs/domain.xml");


//        properties.setConfigFileURI(String.valueOf(domainXml.toURI()));
//        properties.setConfigFileReadOnly(false);
        properties.setPort("http-listener", reposePort);


        glassfish = runtime.newGlassFish(properties);

        glassfish.start();

        ScatteredArchive archive = new ScatteredArchive("repose", ScatteredArchive.Type.WAR);


        File war = new File(reposeRootWar);

        archive.addMetadata(war);
        archive.addMetadata(new File("resources", "sun-web.xml"));

        Deployer deployer = glassfish.getDeployer();
        deployer.deploy(war, "--name=repose", "--contextroot=repose", "--force=true");
    }

}
