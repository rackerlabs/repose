package com.rackspace.cloud.valve.jetty;

import com.rackspace.cloud.valve.jetty.servlet.ControllerServlet;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;

/**
 * TODO: this jetty is started and doing nothing and connected to nothing
 * All it does is fire up our spring, and start an unimplemented Servlet
 */
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

    /**
     * We get spring here.
     * This is where spring is started for valve
     * @param serverReference
     * @return
     */
    private ServletContextHandler buildRootContext(Server serverReference) {
        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
        servletContext.getInitParams()
                .put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);
        servletContext.getInitParams().put(InitParameter.INSECURE.getParameterName(), Boolean.toString(insecure));

        servletContext.getInitParams().put("contextConfigLocation", "classpath:applicationContext.xml");

        servletContext.addEventListener(new ContextLoaderListener());

        return servletContext;
    }
}
