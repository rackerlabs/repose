package com.rackspace.papi;

import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.jetty.JettyServerBuilder;
import com.rackspace.papi.jetty.JettyTestingContext;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.test.DummyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PapiContextExercise extends JettyTestingContext {

    private final static Logger LOG = LoggerFactory.getLogger(PapiContextExercise.class);

    public static void main(String args[]) {

        try {
            new PapiContextExercise().startServer();
        } catch (Exception ex) {
            LOG.warn("Error occurred while running PapiContextExercise", ex);
        }
    }

    public void startServer() throws Exception {
        final JettyServerBuilder server = new JettyServerBuilder(65000);
        buildServerContext(server);
        server.start();

        LOG.info("Server started");
    }

    @Override
    public final void buildServerContext(JettyServerBuilder serverBuilder) throws Exception {
        serverBuilder.addContextListener(PowerApiContextManager.class);
        serverBuilder.addContextInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/etc/powerapi");
        serverBuilder.addFilter(PowerFilter.class, "/*");

        serverBuilder.addServlet(DummyServlet.class, "/*");
    }
}
