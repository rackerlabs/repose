package com.rackspace.papi.test.glassfish;

import com.rackspace.papi.test.ReposeContainer;
import com.rackspace.papi.test.ReposeContainerProps;
import org.glassfish.embeddable.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ReposeGlassFishContainer extends ReposeContainer {
    private static final Logger LOG = LoggerFactory.getLogger(ReposeGlassFishContainer.class);

    private GlassFish glassfish;

    public ReposeGlassFishContainer(ReposeContainerProps props) throws GlassFishException {

        super(props);
        GlassFishProperties properties = new GlassFishProperties();
        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
        properties.setPort("http-listener", Integer.parseInt(listenPort));

        glassfish = runtime.newGlassFish(properties);
    }

    @Override
    protected void startRepose() {
        try {
            glassfish.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    stopRepose();
                }
            });

            File war = new File(warLocation);
            Deployer deployer = glassfish.getDeployer();
            deployer.deploy(war, "--name=repose", "--contextroot=/", "--force=true");
        } catch (GlassFishException e) {
            LOG.trace("Unable to start glassfish container", e);
        }
    }

    @Override
    protected void stopRepose() {
        try {
            glassfish.stop();
        } catch (GlassFishException e) {
            LOG.trace("Unable to stop glassfish container", e);
        }
    }
}
