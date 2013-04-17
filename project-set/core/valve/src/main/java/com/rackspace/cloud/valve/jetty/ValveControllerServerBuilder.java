package com.rackspace.cloud.valve.jetty;

import com.rackspace.cloud.valve.controller.service.context.impl.ReposeValveControllerContextManager;
import com.rackspace.cloud.valve.jetty.servlet.ControllerServlet;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ValveControllerServerBuilder {

    private String configurationPathAndFile = "";
    private final String connectionFramework;
    private final boolean insecure;

    public ValveControllerServerBuilder(String configPath,
            String connectionFramework, boolean insecure) {
        this.configurationPathAndFile = configPath;
        this.connectionFramework = connectionFramework;
        this.insecure = insecure;
    }

    public Server newServer() {

        Server server = new Server();

        final ServletContextHandler rootContext = buildRootContext(server);
        final ServletHolder controllerServer = new ServletHolder(new ControllerServlet());
        rootContext.addServlet(controllerServer, "/*");
        server.setHandler(rootContext);

        return server;
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
        servletContext.getInitParams()
                .put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);
        servletContext.getInitParams().put(InitParameter.CONNECTION_FRAMEWORK.getParameterName(), connectionFramework);
        servletContext.getInitParams().put(InitParameter.INSECURE.getParameterName(), Boolean.toString(insecure));


        try {
            ReposeValveControllerContextManager contextManager =
                    ReposeValveControllerContextManager.class.newInstance();
            servletContext.addEventListener(contextManager);
        } catch (InstantiationException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        } catch (IllegalAccessException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        }

        return servletContext;
    }
}
