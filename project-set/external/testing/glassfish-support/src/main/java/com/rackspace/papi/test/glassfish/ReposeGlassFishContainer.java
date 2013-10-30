package com.rackspace.papi.test.glassfish;

import com.rackspace.papi.test.ContainerMonitorThread;
import com.rackspace.papi.test.ReposeContainer;
import com.rackspace.papi.test.ReposeContainerProps;
import org.glassfish.embeddable.*;

import java.io.File;

public class ReposeGlassFishContainer extends ReposeContainer {

    private GlassFish glassfish;
    protected ContainerMonitorThread monitor;


    public ReposeGlassFishContainer(ReposeContainerProps props) throws GlassFishException {

        super(props);
        monitor = new ContainerMonitorThread(this, Integer.parseInt(stopPort));
        GlassFishProperties properties = new GlassFishProperties();
        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
        properties.setPort("http-listener", Integer.parseInt(listenPort));

        glassfish = runtime.newGlassFish(properties);


    }

    @Override
    protected void startRepose() {

        try {
            glassfish.start();
            monitor.start();
            File war = new File(warLocation);
            Deployer deployer = glassfish.getDeployer();
            deployer.deploy(war, "--name=repose", "--contextroot=/", "--force=true");

        } catch (GlassFishException e) {
            System.err.println("Unable to start glassfish container");
        }
    }

    @Override
    protected void stopRepose() {
        try {
            glassfish.stop();
        } catch (GlassFishException e) {
            System.err.println("Unable to stop glassfish server");
        }
    }
}
