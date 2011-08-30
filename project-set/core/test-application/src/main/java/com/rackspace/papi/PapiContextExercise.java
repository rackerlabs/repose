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

    private static final Logger LOG = LoggerFactory.getLogger(PapiContextExercise.class);
    private static final String DEFAULT_CFG_DIR = "/etc/powerapi";

    public static void main(String args[]) {
        String cfgDir = DEFAULT_CFG_DIR;
        
        if (args.length > 0) {
            for (int argIndex = 0; argIndex < args.length; argIndex++) {
                if (args[argIndex].equals("--conf-dir")) {
                    cfgDir = args[++argIndex];
                }
            }
        }

        try {
            new PapiContextExercise(cfgDir).startServer();
        } catch (Exception ex) {
            LOG.warn("Error occurred while running PapiContextExercise", ex);
        }
    }
    
    private final String configurationDirectory;

    public PapiContextExercise(String configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
    }
    
    public void startServer() throws Exception {
        final JettyServerBuilder server = new JettyServerBuilder(65000);
        buildServerContext(server);
        server.start();

        LOG.info("Server started");
    }

    @Override
    public void buildServerContext(JettyServerBuilder serverBuilder) throws Exception {
        serverBuilder.addContextListener(PowerApiContextManager.class);
        serverBuilder.addContextInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationDirectory);
        serverBuilder.addFilter(PowerFilter.class, "/*");

        serverBuilder.addServlet(DummyServlet.class, "/*");
    }
}
