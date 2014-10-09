package org.openrepose.core.valve.jetty;

import org.openrepose.core.valve.services.controller.impl.ReposeValveControllerContextManager;
import org.openrepose.core.valve.jetty.servlet.ControllerServlet;
import org.openrepose.core.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ValveControllerServerBuilder {

    private String configurationPathAndFile = "";
    private final boolean insecure;

    public ValveControllerServerBuilder(String configPath,
            boolean insecure) {
        this.configurationPathAndFile = configPath;
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
